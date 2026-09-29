package se.sundsvall.operaton.workers.financialaid.rules;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;

/**
 * The answer-quality inputs read out of the SO response. The point of these is the distinction the income list cannot
 * make on its own: an organisation that could not answer and one that answered "nothing here" both contribute zero
 * incomes.
 */
class SsbtekIncomeExtractorAnswerTest {

	private static Map<String, Object> soBasis(final Object... organisations) {
		return Map.of("so", Map.of("ArbetsloshetsersattningLista",
			Map.of("Arbetsloshetsersattning", List.of(organisations))));
	}

	@Test
	void readsOrganisationStatusAndTheStructuralSignals() {
		final var answers = SsbtekIncomeExtractor.extractAnswers(soBasis(Map.of(
			"SvarandeOrganisation", "Unionens a-kassa",
			"StatusSvarandeOrganisation", "1",
			"AnsoktOmErsattning", Map.of("AnsokanOmUtbetalningInlamnad", "false"),
			"Utbetalningar", List.of(Map.of("NettoEfterSkatt", "3200")))));

		assertThat(answers).singleElement().satisfies(answer -> {
			assertThat(answer.agency()).isEqualTo("so");
			assertThat(answer.organisation()).isEqualTo("Unionens a-kassa");
			assertThat(answer.status()).isEqualTo("1");
			assertThat(answer.applicationInfoPresent()).isTrue();
			assertThat(answer.paymentsPresent()).isTrue();
		});
	}

	@Test
	void anOrganisationThatSaidNothingAboutAnApplicationIsNotTheSameAsSayingThereIsNone() {
		final var answers = SsbtekIncomeExtractor.extractAnswers(soBasis(Map.of(
			"SvarandeOrganisation", "Akademikernas a-kassa",
			"StatusSvarandeOrganisation", "9")));

		assertThat(answers).singleElement().satisfies(answer -> {
			assertThat(answer.applicationInfoPresent()).isFalse();
			assertThat(answer.paymentsPresent()).isFalse();
		});
	}

	@Test
	void qualityIsPerOrganisationSoOneCanAnswerWhileAnotherCannot() {
		final var answers = SsbtekIncomeExtractor.extractAnswers(soBasis(
			Map.of("SvarandeOrganisation", "A", "StatusSvarandeOrganisation", "1",
				"Utbetalningar", List.of(Map.of("NettoEfterSkatt", "100"))),
			Map.of("SvarandeOrganisation", "B", "StatusSvarandeOrganisation", "9")));

		assertThat(answers).hasSize(2);
		assertThat(answers.getFirst().paymentsPresent()).isTrue();
		assertThat(answers.getLast().paymentsPresent()).isFalse();
	}

	@Test
	void anOrganisationWithNoPaymentsStillProducesAnAnswerToClassify() {
		// The regression this guards: reading only the income list makes an unanswerable a-kassa invisible.
		final var basis = soBasis(Map.of("SvarandeOrganisation", "Tom", "StatusSvarandeOrganisation", "9"));

		assertThat(SsbtekIncomeExtractor.extract(basis, APPLICANT)).isEmpty();
		assertThat(SsbtekIncomeExtractor.extractAnswers(basis)).hasSize(1);
	}

	@Test
	void missingStatusIsCarriedAsNullRatherThanInvented() {
		final var answers = SsbtekIncomeExtractor.extractAnswers(soBasis(Map.of("SvarandeOrganisation", "Utan status")));

		assertThat(answers).singleElement().satisfies(answer -> assertThat(answer.status()).isNull());
	}

	@Test
	void nullOrEmptyBasisYieldsNoAnswers() {
		assertThat(SsbtekIncomeExtractor.extractAnswers(null)).isEmpty();
		assertThat(SsbtekIncomeExtractor.extractAnswers(Map.of())).isEmpty();
		assertThat(SsbtekIncomeExtractor.extractAnswers(Map.of("so", Map.of()))).isEmpty();
	}
}
