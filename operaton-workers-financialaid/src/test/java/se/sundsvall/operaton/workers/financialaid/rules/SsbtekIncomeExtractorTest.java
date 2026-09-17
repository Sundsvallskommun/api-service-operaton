package se.sundsvall.operaton.workers.financialaid.rules;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.CO_APPLICANT;

class SsbtekIncomeExtractorTest {

	@Test
	void extractsSocialInsuranceAndUnemploymentBenefitPayments() {
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
		final var bostadsbidrag = incomes.stream().filter(i -> "Bostadsbidrag".equals(i.benefit())).findFirst().orElseThrow();
		assertThat(bostadsbidrag.netAmount()).isEqualByComparingTo("1850");
		// "Månad" is the payout method (the regelverk's Typ column), not its Beloppstyp - a payment without detail
		// rows has no amount type to report, and claiming one is what kept the rålista's kvittning rows unreachable
		assertThat(bostadsbidrag.amountType()).isNull();
		assertThat(bostadsbidrag.period()).isEqualTo(LocalDate.of(2026, Month.MAY, 15));
		assertThat(bostadsbidrag.role()).isEqualTo(APPLICANT);
		final var akassa = incomes.stream().filter(i -> "Arbetslöshetsersättning".equals(i.benefit())).findFirst().orElseThrow();
		assertThat(akassa.netAmount()).isEqualByComparingTo("3200");
		assertThat(akassa.period()).isEqualTo(LocalDate.of(2026, Month.MAY, 20));
	}

	@Test
	void liftsSubBenefitAmountTypeAndDaysFromTheSingleDetailRow() {
		final Map<String, Object> basis = Map.of("fk", Map.of("utbetalningar", List.of(Map.of(
			"nettobelopp", Map.of("summa", "4500"),
			"datum", "2026-05-25",
			"period", Map.of("fran", "2026-04-01", "till", "2026-04-30"),
			"formansfamilj", Map.of("beskrivning", "Bostadsbidrag"),
			"typ", Map.of("beskrivning", "Månad"),
			"utbetalningsdetalj", List.of(Map.of(
				"forman", Map.of("beskrivning", "Bostadsbidrag"),
				"beloppstyp", Map.of("beskrivning", "Avdrag Soc"),
				"dagar", 30))))));

		final var income = SsbtekIncomeExtractor.extract(basis, APPLICANT).getFirst();

		assertThat(income.subBenefit()).isEqualTo("Bostadsbidrag");
		assertThat(income.amountType()).isEqualTo("Avdrag Soc");
		assertThat(income.days()).isEqualByComparingTo("30");
		// the payment date and the period it covers are different months - both are carried
		assertThat(income.period()).isEqualTo(LocalDate.of(2026, Month.MAY, 25));
		assertThat(income.periodFrom()).isEqualTo(LocalDate.of(2026, Month.APRIL, 1));
		assertThat(income.periodTo()).isEqualTo(LocalDate.of(2026, Month.APRIL, 30));
		assertThat(income.netAmount()).isEqualByComparingTo("4500");
	}

	@Test
	void leavesDetailFieldsUnsetWhenThePaymentIsSplitOverSeveralRows() {
		final Map<String, Object> basis = Map.of("fk", Map.of("utbetalningar", List.of(Map.of(
			"nettobelopp", Map.of("summa", "4500"),
			"datum", "2026-05-25",
			"period", Map.of("fran", "2026-04-01", "till", "2026-04-30"),
			"formansfamilj", Map.of("beskrivning", "Bostadsbidrag"),
			"utbetalningsdetalj", List.of(
				Map.of("forman", Map.of("beskrivning", "Bostadsbidrag"), "beloppstyp", Map.of("beskrivning", "Preliminärt bostadsbidrag"), "dagar", 30),
				Map.of("forman", Map.of("beskrivning", "Bostadsbidrag"), "beloppstyp", Map.of("beskrivning", "Avdrag Soc"), "dagar", 30))))));

		final var income = SsbtekIncomeExtractor.extract(basis, APPLICANT).getFirst();

		// an amount split across rows has no single sub-benefit or amount type; guessing one would misclassify it
		assertThat(income.subBenefit()).isNull();
		assertThat(income.amountType()).isNull();
		assertThat(income.days()).isNull();
		// the period still comes from the payment itself, so it survives the split
		assertThat(income.periodFrom()).isEqualTo(LocalDate.of(2026, Month.APRIL, 1));
		assertThat(income.netAmount()).isEqualByComparingTo("4500");
	}

	@Test
	void fallsBackToTheLetterCodeAndToleratesAnUnreadableDayCount() {
		final Map<String, Object> basis = Map.of("fk", Map.of("utbetalningar", List.of(Map.of(
			"nettobelopp", Map.of("summa", "1000"),
			"datum", "2026-05-25",
			"formansfamilj", Map.of("beskrivning", "Dagersättning"),
			"utbetalningsdetalj", List.of(Map.of(
				"forman", Map.of("id", "FP"), // no beskrivning → fall back to the code
				"beloppstyp", Map.of("id", "AFR"),
				"dagar", "inte ett tal"))))));

		final var income = SsbtekIncomeExtractor.extract(basis, APPLICANT).getFirst();

		assertThat(income.subBenefit()).isEqualTo("FP");
		assertThat(income.amountType()).isEqualTo("AFR");
		assertThat(income.days()).isNull();
		assertThat(income.periodFrom()).isNull();
	}

	@Test
	void carriesThePeriodAndCompensationDaysFromTheUnemploymentFund() {
		final Map<String, Object> basis = Map.of("so", Map.of("ArbetsloshetsersattningLista", Map.of("Arbetsloshetsersattning", List.of(
			Map.of("Utbetalningar", List.of(Map.of(
				"NettoEfterSkatt", "3200",
				"Utbetalningsdatum", "2026-05-20",
				"AvserFrom", "2026-04-01",
				"AvserTom", "2026-04-30",
				"Ersattningsdagar", "22")))))));

		final var income = SsbtekIncomeExtractor.extract(basis, APPLICANT).getFirst();

		assertThat(income.periodFrom()).isEqualTo(LocalDate.of(2026, Month.APRIL, 1));
		assertThat(income.periodTo()).isEqualTo(LocalDate.of(2026, Month.APRIL, 30));
		// the schema types this as a decimal; it is a day count
		assertThat(income.days()).isEqualByComparingTo("22");
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
		assertThat(incomes.getFirst().benefit()).isEqualTo("PM");
		assertThat(incomes.getFirst().amountType()).isNull();
		assertThat(incomes.getFirst().role()).isEqualTo(CO_APPLICANT);
	}

	@Test
	void toleratesBadAmountAndUnparsableDates() {
		final Map<String, Object> basis = Map.of("fk", Map.of("utbetalningar", List.of(
			Map.of("nettobelopp", Map.of("summa", "abc"), "datum", "2026-05-01", "formansfamilj", Map.of("beskrivning", "X")), // bad amount → skipped
			Map.of("nettobelopp", Map.of("summa", "2000"), "datum", "2026", "formansfamilj", Map.of("beskrivning", "Dagersättning")),       // short date → null period
			Map.of("nettobelopp", Map.of("summa", "1000"), "datum", "2026-13-45", "formansfamilj", Map.of("beskrivning", "Barnbidrag")))));  // invalid date → null period

		final var incomes = SsbtekIncomeExtractor.extract(basis, APPLICANT);

		assertThat(incomes)
			.hasSize(2)
			.allSatisfy(income -> assertThat(income.period()).isNull())
			.extracting(SsbtekIncome::benefit).containsExactlyInAnyOrder("Dagersättning", "Barnbidrag");
	}
}
