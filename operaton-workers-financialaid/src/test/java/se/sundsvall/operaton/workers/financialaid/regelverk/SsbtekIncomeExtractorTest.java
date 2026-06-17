package se.sundsvall.operaton.workers.financialaid.regelverk;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.operaton.workers.financialaid.regelverk.ApplicantRole.APPLICANT;
import static se.sundsvall.operaton.workers.financialaid.regelverk.ApplicantRole.CO_APPLICANT;

class SsbtekIncomeExtractorTest {

	@Test
	void extractsFkUtbetalningarAndArbetsloshetsersattning() {
		final Map<String, Object> basis = Map.of(
			"fk", Map.of("utbetalningar", List.of(Map.of(
				"nettobelopp", Map.of("summa", "1850"),
				"datum", "2026-05-15",
				"formansfamilj", Map.of("beskrivning", "Bostadsbidrag"),
				"typ", Map.of("beskrivning", "Månad")))),
			"so", Map.of("ArbetsloshetsersattningLista", Map.of("Arbetsloshetsersattning", List.of(
				Map.of("Utbetalningar", List.of(Map.of("NettoEfterSkatt", "3200", "Utbetalningsdatum", "2026-05-20")))))));

		final var incomes = SsbtekIncomeExtractor.extract(basis, APPLICANT);

		assertThat(incomes).hasSize(2);
		final var bostadsbidrag = incomes.stream().filter(i -> "Bostadsbidrag".equals(i.forman())).findFirst().orElseThrow();
		assertThat(bostadsbidrag.netAmount()).isEqualByComparingTo("1850");
		assertThat(bostadsbidrag.beloppstyp()).isEqualTo("Månad");
		assertThat(bostadsbidrag.period()).isEqualTo(LocalDate.of(2026, 5, 15));
		assertThat(bostadsbidrag.role()).isEqualTo(APPLICANT);
		final var akassa = incomes.stream().filter(i -> "Arbetslöshetsersättning".equals(i.forman())).findFirst().orElseThrow();
		assertThat(akassa.netAmount()).isEqualByComparingTo("3200");
		assertThat(akassa.period()).isEqualTo(LocalDate.of(2026, 5, 20));
	}

	@Test
	void nullBasisYieldsEmpty() {
		assertThat(SsbtekIncomeExtractor.extract(null, APPLICANT)).isEmpty();
	}

	@Test
	void singleObjectIdFallbackAndAmountlessSkip() {
		final Map<String, Object> withId = new HashMap<>();
		withId.put("nettobelopp", Map.of("summa", "500"));
		withId.put("datum", "2026-05-01");
		withId.put("formansfamilj", Map.of("id", "PM")); // no beskrivning → fall back to id

		final Map<String, Object> basis = Map.of("fk", Map.of("utbetalningar", List.of(
			withId,
			Map.of("formansfamilj", Map.of("beskrivning", "Skip"))))); // no amount → skipped

		final var incomes = SsbtekIncomeExtractor.extract(basis, CO_APPLICANT);

		assertThat(incomes).hasSize(1);
		assertThat(incomes.getFirst().forman()).isEqualTo("PM");
		assertThat(incomes.getFirst().beloppstyp()).isNull();
		assertThat(incomes.getFirst().role()).isEqualTo(CO_APPLICANT);
	}

	@Test
	void toleratesBadAmountAndUnparsableDates() {
		final Map<String, Object> basis = Map.of("fk", Map.of("utbetalningar", List.of(
			Map.of("nettobelopp", Map.of("summa", "abc"), "datum", "2026-05-01", "formansfamilj", Map.of("beskrivning", "X")), // bad amount → skipped
			Map.of("nettobelopp", Map.of("summa", "2000"), "datum", "2026", "formansfamilj", Map.of("beskrivning", "Dagersättning")),       // short date → null period
			Map.of("nettobelopp", Map.of("summa", "1000"), "datum", "2026-13-45", "formansfamilj", Map.of("beskrivning", "Barnbidrag")))));  // invalid date → null period

		final var incomes = SsbtekIncomeExtractor.extract(basis, APPLICANT);

		assertThat(incomes).hasSize(2);
		assertThat(incomes).allSatisfy(income -> assertThat(income.period()).isNull());
		assertThat(incomes).extracting(SsbtekIncome::forman).containsExactlyInAnyOrder("Dagersättning", "Barnbidrag");
	}
}
