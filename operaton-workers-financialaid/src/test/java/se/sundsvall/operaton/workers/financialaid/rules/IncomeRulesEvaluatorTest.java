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
