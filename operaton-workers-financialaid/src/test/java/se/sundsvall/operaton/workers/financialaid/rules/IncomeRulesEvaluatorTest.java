package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.dmn.DecisionsEvaluationBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;
import static se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator.INCOME_ALLOW_LIST_DECISION_KEY;
import static se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator.INCOME_THRESHOLD_DECISION_KEY;

@ExtendWith(MockitoExtension.class)
class IncomeRulesEvaluatorTest {

	@Mock
	private DecisionService decisionServiceMock;

	@InjectMocks
	private IncomeRulesEvaluator evaluator;

	private void stubDecision(final String key, final Map<String, Object> resultRow) {
		final var builder = mock(DecisionsEvaluationBuilder.class);
		final var result = mock(DmnDecisionResult.class);
		when(decisionServiceMock.evaluateDecisionByKey(key)).thenReturn(builder);
		when(builder.variables(anyMap())).thenReturn(builder);
		when(builder.evaluate()).thenReturn(result);
		when(result.getResultList()).thenReturn(List.of(resultRow));
	}

	/** The threshold table as verksamheten published it 2026-09-17: exact comparison plus the warning text. */
	private void stubExactThreshold(final String rule) {
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 0, "riktning", "ner_upp", "regel", rule));
	}

	private static SsbtekIncome income(final String benefit, final String period, final String amount) {
		return new SsbtekIncome(benefit, null, null, new BigDecimal(amount), LocalDate.parse(period), APPLICANT);
	}

	/** An income that carries both a payment date and a covered period, so the two can disagree. */
	private static SsbtekIncome incomeForPeriod(final String benefit, final String paidOn, final String from, final String to, final String amount) {
		return new SsbtekIncome(benefit, null, null, new BigDecimal(amount), LocalDate.parse(paidOn),
			LocalDate.parse(from), LocalDate.parse(to), null, APPLICANT);
	}

	@Test
	void attributesAnIncomeToTheDayItWasPaidRatherThanThePeriodItCovers() {
		// verksamheten 2026-09-21: "det är utbetalningsdatumet som styr". Paid 2 October for September, so it is
		// October's money and falls outside both rule periods for an October application.
		final var result = evaluator.evaluate(
			List.of(incomeForPeriod("Bostadsbidrag", "2026-10-02", "2026-09-01", "2026-09-30", "4500")),
			YearMonth.of(2026, Month.OCTOBER));

		assertThat(result.classified()).isEmpty();
	}

	@Test
	void usesThePaymentDateWhenThePayloadCarriesNoPeriodAtAll() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Barnbidrag", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12));

		// payments split over several detail rows carry no single period; nothing changes for them
		final var result = evaluator.evaluate(
			List.of(income("Allmänt barnbidrag", "2026-09-20", "1250")),
			YearMonth.of(2026, Month.OCTOBER));

		assertThat(result.classified()).hasSize(1);
	}

	@Test
	void aPaymentMadeInTheControlPeriodCountsThereEvenWhenItCoversAnotherMonth() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12));

		// paid 28 September (the control period for an October application) but covering October - the payment
		// date wins, so it is transferred. This is the exact case that reversed on 2026-09-21.
		final var result = evaluator.evaluate(
			List.of(incomeForPeriod("Bostadsbidrag", "2026-09-28", "2026-10-01", "2026-10-31", "4500")),
			YearMonth.of(2026, Month.OCTOBER));

		assertThat(result.classified()).hasSize(1);
		assertThat(result.classified().getFirst().income().benefit()).isEqualTo("Bostadsbidrag");
	}

	@Test
	void classifiesTransferableAndDetectsChangeOverThreshold() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED_KVITTNING", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med kvittning"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12));

		final var result = evaluator.evaluate(List.of(
			income("Bostadsbidrag", "2026-05-15", "1850"),
			income("Bostadsbidrag", "2026-04-15", "2400")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.classified()).hasSize(1);
		assertThat(result.classified().getFirst().action()).isEqualTo("TA_MED_KVITTNING");
		assertThat(result.classified().getFirst().calculation()).isEqualTo("Bostadsbidrag");
		assertThat(result.changeWarnings()).hasSize(1);
		assertThat(result.changeWarnings().getFirst().benefit()).isEqualTo("Bostadsbidrag");
		assertThat(result.changeWarnings().getFirst().changePercent()).isEqualByComparingTo("-23");
	}

	@Test
	void anExactThresholdWarnsOnSumsThatDifferByLessThanARoundedPercent() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Barnbidrag", "varning", false, "regel", "Ta med"));
		stubExactThreshold("Barnbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");

		// 1250 -> 1255 kr is 0,4 % and rounds to 0 %; the exact comparison is what catches it
		final var result = evaluator.evaluate(List.of(
			income("Allmänt barnbidrag", "2026-05-20", "1255"),
			income("Allmänt barnbidrag", "2026-04-20", "1250")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).hasSize(1);
		final var warning = result.changeWarnings().getFirst();
		assertThat(warning.benefit()).isEqualTo("Allmänt barnbidrag");
		assertThat(warning.changePercent()).isEqualByComparingTo("0");
		assertThat(warning.comparisonSum()).isEqualByComparingTo("1250");
		assertThat(warning.controlSum()).isEqualByComparingTo("1255");
		assertThat(warning.rule()).isEqualTo("Barnbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");
	}

	@Test
	void anExactThresholdIsQuietWhenTheSumsAreTheSame() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Barnbidrag", "varning", false, "regel", "Ta med"));
		stubExactThreshold("Barnbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");

		// same amount, different scale - "samma summa" is about the value, not how SSBTEK wrote it
		final var result = evaluator.evaluate(List.of(
			income("Allmänt barnbidrag", "2026-05-20", "1250.00"),
			income("Allmänt barnbidrag", "2026-04-20", "1250")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).isEmpty();
	}

	@Test
	void anExactThresholdTreatsAMissingComparisonSumAsZero() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med"));
		stubExactThreshold("Bostadsbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");

		// the benefit is new this month: nothing in the comparison period, so that side is 0
		final var result = evaluator.evaluate(List.of(
			income("Bostadsbidrag", "2026-05-15", "4500")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).hasSize(1);
		final var warning = result.changeWarnings().getFirst();
		assertThat(warning.comparisonSum()).isEqualByComparingTo("0");
		assertThat(warning.controlSum()).isEqualByComparingTo("4500");
		// there is no comparison sum to take a percentage of
		assertThat(warning.changePercent()).isNull();
	}

	@Test
	void aVanishedBenefitIsNotAChangeWarningButIsCarriedForTheMissingIncomeWarning() {
		// Verksamhetens reviderade regelverk (2026-09-21) wants "INKOMST fanns föregående månad i SSBTEK men saknas
		// nu", raised by caremanagement off jamforelseperiod. Warning here as well would give the handläggare two rows
		// for one fact, and -100 % reads as a measured change rather than as an answer we never got.
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Underhållsstöd", "varning", false, "regel", "Ta med"));

		final var result = evaluator.evaluate(List.of(
			income("Underhållsstöd", "2026-04-20", "1673")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).isEmpty();
		assertThat(result.classified()).singleElement().satisfies(classified -> {
			assertThat(classified.income().benefit()).isEqualTo("Underhållsstöd");
			assertThat(classified.fromComparisonPeriod()).isTrue();
		});
	}

	@Test
	void aBenefitThatIsNewThisMonthStillWarns() {
		// The opposite direction is untouched: nothing in the comparison period, an amount in the control period.
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Underhållsstöd", "varning", false, "regel", "Ta med"));
		stubExactThreshold("Underhållsstöd föregående månad är inte samma summa som denna månad – kontrollera summan");

		final var result = evaluator.evaluate(List.of(
			income("Underhållsstöd", "2026-05-20", "1673")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).hasSize(1);
		final var warning = result.changeWarnings().getFirst();
		assertThat(warning.benefit()).isEqualTo("Underhållsstöd");
		assertThat(warning.comparisonSum()).isEqualByComparingTo("0");
		assertThat(warning.controlSum()).isEqualByComparingTo("1673");
	}

	@Test
	void aPercentThresholdStillSkipsTheBenefitWithNothingToComparePercentAgainst() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Dagersättning", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12, "regel", "Dagersättning skiljer sig mer än 12 %"));

		// only in the control period, and a percentage of zero is undefined - the 12 % benefits keep being skipped
		final var result = evaluator.evaluate(List.of(
			income("Dagersättning", "2026-05-10", "5000")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).isEmpty();
	}

	@Test
	void toleratesAThresholdTableWithNoRuleText() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med"));
		// the table published before 2026-09-17 has no regel output at all
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12, "riktning", "ner_upp"));

		final var result = evaluator.evaluate(List.of(
			income("Bostadsbidrag", "2026-05-15", "1850"),
			income("Bostadsbidrag", "2026-04-15", "2400")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).hasSize(1);
		assertThat(result.changeWarnings().getFirst().rule()).isNull();
	}

	@Test
	void fallsBackToTheDefaultThresholdWhenTheTableGivesNoPercent() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("riktning", "ner_upp"));

		// no troskelProcent at all falls back to 12 %, so a 10 % change stays quiet
		final var result = evaluator.evaluate(List.of(
			income("Bostadsbidrag", "2026-05-15", "2200"),
			income("Bostadsbidrag", "2026-04-15", "2000")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).isEmpty();
	}

	@Test
	void transfersComparisonFallbackAndSkipsWarningUnderThreshold() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Dagersättning", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12));

		final var result = evaluator.evaluate(List.of(
			income("Dagersättning", "2026-05-10", "5000"),
			income("Dagersättning", "2026-04-10", "4800"),
			income("Barnbidrag", "2026-04-12", "1250")),
			YearMonth.of(2026, Month.JUNE));

		// Dagersättning moved 4800 -> 5000 (+4 %, under the 12 % tolerance). Barnbidrag exists only in the comparison
		// period, so it is carried as a fallback and left to caremanagement's "saknas nu" warning rather than being
		// reported here as a -100 % change.
		assertThat(result.classified()).hasSize(2);
		assertThat(result.classified()).filteredOn(classified -> "Barnbidrag".equals(classified.income().benefit()))
			.singleElement()
			.satisfies(classified -> assertThat(classified.fromComparisonPeriod()).isTrue());
		assertThat(result.changeWarnings()).isEmpty();
	}

	@Test
	void marksWhichIncomesCameFromTheComparisonPeriod() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12));

		// application month June → control May, comparison April. Underhållsstöd only exists in April.
		final var result = evaluator.evaluate(List.of(
			income("Bostadsbidrag", "2026-05-15", "1850"),
			income("Underhållsstöd", "2026-04-20", "1673")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.classified()).hasSize(2);
		final var control = result.classified().stream().filter(c -> "Bostadsbidrag".equals(c.income().benefit())).findFirst().orElseThrow();
		final var fallback = result.classified().stream().filter(c -> "Underhållsstöd".equals(c.income().benefit())).findFirst().orElseThrow();
		assertThat(control.fromComparisonPeriod()).isFalse();
		// caremanagement decides whether this one was already taken last month; the engine only says where it came from
		assertThat(fallback.fromComparisonPeriod()).isTrue();
	}

	@Test
	void nullIncomesYieldEmptyResult() {
		final var result = evaluator.evaluate(null, YearMonth.of(2026, Month.JUNE));

		assertThat(result.classified()).isEmpty();
		assertThat(result.changeWarnings()).isEmpty();
	}

	@Test
	void aNegativeThresholdMeansTheBenefitIsNotComparedAtAll() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Dagersättning", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", -1, "regel", "Dagersättning jämförs inte mot föregående månad"));

		// 1000 → 5000 is a 400 % change; with no comparison at all it still says nothing
		final var result = evaluator.evaluate(List.of(
			income("Dagersättning", "2026-04-15", "1000"),
			income("Dagersättning", "2026-05-15", "5000")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.classified()).hasSize(1);
		assertThat(result.changeWarnings()).isEmpty();
	}

	@Test
	void aNegativeThresholdIsNotTheSameAsAWideTolerance() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Barnbidrag", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 0, "regel", "Barnbidrag är inte samma summa"));

		// the same shape with an exact threshold does warn, so the silence above comes from -1 and nothing else
		final var result = evaluator.evaluate(List.of(
			income("Allmänt barnbidrag", "2026-04-15", "1000"),
			income("Allmänt barnbidrag", "2026-05-15", "5000")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.changeWarnings()).hasSize(1);
	}

	@Test
	void passesTheNetAmountToTheAllowListSoTheAmountOnlyRulesCanFire() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "EJ_TA_MED", "normberakning", "-", "varning", false, "regel", "Extratillägg"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 0));
		final var variables = ArgumentCaptor.forClass(Map.class);

		evaluator.evaluate(List.of(income("Studiehjalp", "2026-05-15", "855")), YearMonth.of(2026, Month.JUNE));

		verify(decisionServiceMock.evaluateDecisionByKey(INCOME_ALLOW_LIST_DECISION_KEY), atLeastOnce()).variables(variables.capture());
		assertThat(variables.getAllValues()).anySatisfy(vars -> assertThat(vars)
			.containsEntry("forman", "Studiehjalp")
			.containsEntry("belopp", new BigDecimal("855")));
	}

	@Test
	void sendsANullAmountRatherThanOmittingItWhenSsbtekReportedNoSum() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Barnbidrag", "varning", false, "regel", "Ta med"));
		final var variables = ArgumentCaptor.forClass(Map.class);

		evaluator.evaluate(List.of(new SsbtekIncome("Allmänt barnbidrag", null, null, null, LocalDate.parse("2026-05-15"), APPLICANT)),
			YearMonth.of(2026, Month.JUNE));

		verify(decisionServiceMock.evaluateDecisionByKey(INCOME_ALLOW_LIST_DECISION_KEY), atLeastOnce()).variables(variables.capture());
		// a null amount must reach the table as null, not be left out: an absent key and a null read the same in FEEL,
		// but only an explicit null keeps the caller honest about having looked
		assertThat(variables.getAllValues()).anySatisfy(vars -> assertThat(vars).containsEntry("belopp", null));
	}
}
