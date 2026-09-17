package se.sundsvall.operaton.workers.financialaid.rules;

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
import static se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator.ANSWER_QUALITY_DECISION_KEY;

/**
 * Classification of how far a responding organisation's answer can be trusted.
 * <p>
 * Every test here is ultimately the same assertion: nothing may quietly become {@code SVARAT}. An unknown code, a
 * missing table and an engine failure must all end up as "kunde inte kontrolleras", because the alternative is a
 * normberäkning that silently claims the sökande has no a-kassa income.
 */
@ExtendWith(MockitoExtension.class)
class IncomeRulesEvaluatorAnswerQualityTest {

	private static final YearMonth MONTH = YearMonth.of(2026, 10);

	@Mock
	private DecisionService decisionServiceMock;

	@InjectMocks
	private IncomeRulesEvaluator evaluator;

	private static AgencyAnswer answer() {
		return new AgencyAnswer("so", "Unionens a-kassa", "1", false, false);
	}

	private void stubAnswerQuality(final Map<String, Object> row) {
		final var builder = mock(DecisionsEvaluationBuilder.class);
		final var result = mock(DmnDecisionResult.class);
		when(decisionServiceMock.evaluateDecisionByKey(ANSWER_QUALITY_DECISION_KEY)).thenReturn(builder);
		when(builder.variables(anyMap())).thenReturn(builder);
		when(builder.evaluate()).thenReturn(result);
		when(result.getResultList()).thenReturn(List.of(row));
	}

	@Test
	void carriesTheVerdictAndRuleTextFromTheTable() {
		stubAnswerQuality(Map.of("kvalitet", "SVARAT", "regel", "Organisationen har lämnat utbetalningar"));

		final var result = evaluator.evaluate(List.of(), List.of(answer()), MONTH);

		assertThat(result.answers()).singleElement().satisfies(classified -> {
			assertThat(classified.quality()).isEqualTo("SVARAT");
			assertThat(classified.rule()).isEqualTo("Organisationen har lämnat utbetalningar");
			assertThat(classified.unverifiable()).isFalse();
			assertThat(classified.answer().organisation()).isEqualTo("Unionens a-kassa");
		});
	}

	@Test
	void anUnverifiableVerdictIsFlagged() {
		stubAnswerQuality(Map.of("kvalitet", "EJ_KONTROLLERBAR", "regel", "Statuskodens innebörd är inte fastställd"));

		assertThat(evaluator.evaluate(List.of(), List.of(answer()), MONTH).answers())
			.singleElement()
			.satisfies(classified -> assertThat(classified.unverifiable()).isTrue());
	}

	@Test
	void aTableThatReturnsNoQualityDegradesToUnverifiable() {
		stubAnswerQuality(Map.of("regel", "rad utan kvalitet"));

		assertThat(evaluator.evaluate(List.of(), List.of(answer()), MONTH).answers())
			.singleElement()
			.satisfies(classified -> assertThat(classified.quality()).isEqualTo("EJ_KONTROLLERBAR"));
	}

	@Test
	void aBlankQualityDegradesToUnverifiable() {
		stubAnswerQuality(Map.of("kvalitet", "   "));

		assertThat(evaluator.evaluate(List.of(), List.of(answer()), MONTH).answers())
			.singleElement()
			.satisfies(classified -> assertThat(classified.quality()).isEqualTo("EJ_KONTROLLERBAR"));
	}

	@Test
	void anUndeployedTableDegradesInsteadOfFailingTheWholeEvaluation() {
		// Deploy ordering must not be able to take the income rules down, and a missing regelverk is not a clean bill
		// of health.
		when(decisionServiceMock.evaluateDecisionByKey(ANSWER_QUALITY_DECISION_KEY))
			.thenThrow(new IllegalStateException("no decision definition deployed with key"));

		final var result = evaluator.evaluate(List.of(), List.of(answer()), MONTH);

		assertThat(result.answers()).singleElement().satisfies(classified -> {
			assertThat(classified.quality()).isEqualTo("EJ_KONTROLLERBAR");
			assertThat(classified.rule()).isEqualTo("Regelverket för svarskvalitet kunde inte utvärderas");
		});
	}

	@Test
	void noAnswersMeansNothingWasCheckedNotThatEverythingAnswered() {
		assertThat(evaluator.evaluate(List.of(), List.of(), MONTH).answers()).isEmpty();
		assertThat(evaluator.evaluate(List.of(), null, MONTH).answers()).isEmpty();
	}

	@Test
	void theIncomeOnlyOverloadClassifiesNoAnswers() {
		assertThat(evaluator.evaluate(List.of(), MONTH).answers()).isEmpty();
	}
}
