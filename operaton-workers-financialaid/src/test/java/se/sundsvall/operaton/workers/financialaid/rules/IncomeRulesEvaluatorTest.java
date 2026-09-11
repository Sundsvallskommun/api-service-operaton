package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.dmn.DecisionsEvaluationBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
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

	private static SsbtekIncome income(final String benefit, final String period, final String amount) {
		return new SsbtekIncome(benefit, null, null, new BigDecimal(amount), LocalDate.parse(period), APPLICANT);
	}

	/** An income attributed by the period it covers rather than by the day it was paid. */
	private static SsbtekIncome incomeForPeriod(final String benefit, final String paidOn, final String from, final String to, final String amount) {
		return new SsbtekIncome(benefit, null, null, new BigDecimal(amount), LocalDate.parse(paidOn),
			LocalDate.parse(from), LocalDate.parse(to), null, APPLICANT);
	}

	@Test
	void attributesAnIncomeToThePeriodItCoversRatherThanTheDayItWasPaid() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Bostadsbidrag", "varning", false, "regel", "Ta med"));

		// application month October → control period September. Paid 2 October, but it is September's money.
		final var result = evaluator.evaluate(
			List.of(incomeForPeriod("Bostadsbidrag", "2026-10-02", "2026-09-01", "2026-09-30", "4500")),
			YearMonth.of(2026, Month.OCTOBER));

		// on the payment date alone this landed in October and was transferred to no period at all
		assertThat(result.classified()).hasSize(1);
		assertThat(result.classified().getFirst().income().benefit()).isEqualTo("Bostadsbidrag");
	}

	@Test
	void stillFallsBackToThePaymentDateWhenThePayloadCarriesNoPeriod() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Barnbidrag", "varning", false, "regel", "Ta med"));

		// payments split over several detail rows carry no single period; the payment date has to stand in
		final var result = evaluator.evaluate(
			List.of(income("Allmänt barnbidrag", "2026-09-20", "1250")),
			YearMonth.of(2026, Month.OCTOBER));

		assertThat(result.classified()).hasSize(1);
	}

	@Test
	void aPaymentCoveringAnEarlierMonthDoesNotCountAsThisMonthsIncome() {
		// paid during the control period but covering the application month itself → outside both rule periods
		final var result = evaluator.evaluate(
			List.of(incomeForPeriod("Bostadsbidrag", "2026-09-28", "2026-10-01", "2026-10-31", "4500")),
			YearMonth.of(2026, Month.OCTOBER));

		assertThat(result.classified()).isEmpty();
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
	void transfersComparisonFallbackAndSkipsWarningUnderThreshold() {
		stubDecision(INCOME_ALLOW_LIST_DECISION_KEY, Map.of("atgard", "TA_MED", "normberakning", "Dagersättning", "varning", false, "regel", "Ta med"));
		stubDecision(INCOME_THRESHOLD_DECISION_KEY, Map.of("troskelProcent", 12));

		final var result = evaluator.evaluate(List.of(
			income("Dagersättning", "2026-05-10", "5000"),
			income("Dagersättning", "2026-04-10", "4800"),
			income("Barnbidrag", "2026-04-12", "1250")),
			YearMonth.of(2026, Month.JUNE));

		assertThat(result.classified()).hasSize(2);
		assertThat(result.changeWarnings()).extracting(ChangeWarning::benefit).containsExactly("Barnbidrag");
	}

	@Test
	void nullIncomesYieldEmptyResult() {
		final var result = evaluator.evaluate(null, YearMonth.of(2026, Month.JUNE));

		assertThat(result.classified()).isEmpty();
		assertThat(result.changeWarnings()).isEmpty();
	}
}
