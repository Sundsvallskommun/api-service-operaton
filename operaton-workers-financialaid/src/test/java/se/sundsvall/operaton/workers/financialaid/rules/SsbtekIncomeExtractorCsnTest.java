package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;

/**
 * CSN study support extraction. financial-aid's generic XML-to-JSON conversion means a single ärende or
 * {@code Utbetaldtid} arrives as an object, several as a list, and an empty sub-tree (no studiehjälp, say) arrives
 * as {@code null} - every fixture below exercises one of those shapes.
 */
class SsbtekIncomeExtractorCsnTest {

	private static Map<String, Object> personBasis(final Map<String, Object> person) {
		return Map.of("csn", Map.of("Personer", Map.of("Person", person)));
	}

	@Test
	void extractsASingleArendeGivenAsAnObjectNotAList() {
		final Map<String, Object> person = Map.of("Studiemedel", Map.of("Arenden", Map.of("Arende", Map.of(
			"stodform", "TLÅN",
			"klartext", "Studiemedel för studier i Sverige",
			"status", "BESLUTAD",
			"UtbetalningsPlan", Map.of("Utbetalning", Map.of(
				"utbetdatum", "20260824",
				"utbetstatus", "Utbetald",
				"totbelopp", "16990",
				"Utbetaldatider", Map.of("Utbetaldtid", Map.of(
					"startvecka", "202635",
					"slutvecka", "202639",
					"totbelopp", "16990",
					"Beloppen", Map.of("Belopp", Map.of(
						"beloppstyp", "GRUNDF",
						"klartext", "Förhöjt bidrag",
						"belopp", "16990",
						"totbelopp", "16990"))))))))));

		final var income = SsbtekIncomeExtractor.extract(personBasis(person), APPLICANT).getFirst();

		assertThat(income.benefit()).isEqualTo("Studiemedel");
		assertThat(income.subBenefit()).isEqualTo("Studiemedel för studier i Sverige");
		assertThat(income.amountType()).isEqualTo("Förhöjt bidrag");
		// the payment's own totbelopp, not a sum of the (here, single) Belopp row - same value here either way,
		// which is exactly why a test that only had one Belopp row could hide a wrongly-summing implementation
		assertThat(income.netAmount()).isEqualByComparingTo("16990");
		assertThat(income.period()).isEqualTo(LocalDate.of(2026, Month.AUGUST, 24));
		// covered weeks 202635-202639: Monday of ISO week 35 2026 to Sunday of ISO week 39 2026
		assertThat(income.periodFrom()).isEqualTo(LocalDate.of(2026, Month.AUGUST, 24));
		assertThat(income.periodTo()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 27));
		assertThat(income.days()).isNull();
		assertThat(income.role()).isEqualTo(APPLICANT);
	}

	@Test
	void extractsSeveralArendenGivenAsAListAndIncludesPlanneradPayments() {
		final Map<String, Object> person = Map.of("Studiehjalp", Map.of("Arenden", Map.of("Arende", List.of(
			Map.of(
				"stodform", "GRUND",
				"klartext", "Studiehjälp",
				"UtbetalningsPlan", Map.of("Utbetalning", Map.of(
					"utbetdatum", "20260910",
					"utbetstatus", "Utbetald",
					"totbelopp", "1050"))),
			Map.of(
				"stodform", "TILL",
				"klartext", "Extra tillägg",
				"UtbetalningsPlan", Map.of("Utbetalning", Map.of(
					"utbetdatum", "20261010",
					"utbetstatus", "Planerad",
					"totbelopp", "855")))))));

		final var incomes = SsbtekIncomeExtractor.extract(personBasis(person), APPLICANT);

		// both ärenden's payments are included, and so is the Planerad one - utbetstatus is deliberately not a filter
		assertThat(incomes).hasSize(2);
		// the rålista really does spell this without the umlaut
		assertThat(incomes).extracting(SsbtekIncome::benefit).containsExactly("Studiehjalp", "Studiehjalp");
		assertThat(incomes).extracting(SsbtekIncome::subBenefit).containsExactlyInAnyOrder("Studiehjälp", "Extra tillägg");
		assertThat(incomes).extracting(SsbtekIncome::netAmount)
			.containsExactlyInAnyOrder(new BigDecimal("1050"), new BigDecimal("855"));
	}

	@Test
	void utbetdatumZeroMeansNoDateYetButTheWeekDerivedPeriodStillPlacesThePayment() {
		final Map<String, Object> person = Map.of("Studiemedel", Map.of("Arenden", Map.of("Arende", Map.of(
			"klartext", "Studiemedel för studier i Sverige",
			"UtbetalningsPlan", Map.of("Utbetalning", Map.of(
				"utbetdatum", "0",
				"utbetstatus", "Planerad",
				"totbelopp", "5000",
				"Utbetaldatider", Map.of("Utbetaldtid", Map.of(
					"startvecka", "202701",
					"slutvecka", "202704"))))))));

		final var income = SsbtekIncomeExtractor.extract(personBasis(person), APPLICANT).getFirst();

		// the literal "0" means "not yet scheduled" and must become null, not a bogus date
		assertThat(income.period()).isNull();
		assertThat(income.periodFrom()).isEqualTo(LocalDate.of(2027, Month.JANUARY, 4));
		assertThat(income.periodTo()).isEqualTo(LocalDate.of(2027, Month.JANUARY, 31));
		// the payment date decides since 2026-09-21, but there is none here, so attributionDate() falls back to
		// periodFrom and a dateless-but-scheduled payment still lands in a rule period rather than vanishing
		assertThat(income.attributionDate()).isEqualTo(income.periodFrom());
	}

	@Test
	void emptyStudySupportSubTreeConvertsToNullAndDoesNotThrow() {
		final var person = new HashMap<String, Object>();
		person.put("Studiemedel", Map.of("Arenden", Map.of("Arende", Map.of(
			"klartext", "Studiemedel för studier i Sverige",
			"UtbetalningsPlan", Map.of("Utbetalning", Map.of("utbetdatum", "20260601", "totbelopp", "5000"))))));
		person.put("Studiehjalp", null); // an empty XML element converts to null, not an empty map

		final var incomes = SsbtekIncomeExtractor.extract(personBasis(person), APPLICANT);

		assertThat(incomes).hasSize(1);
		assertThat(incomes.getFirst().benefit()).isEqualTo("Studiemedel");
	}

	@Test
	void mapsStudiestartsstodAndOmstallningsstudiestodToTheirRalistaOrCatchAllStrings() {
		final Map<String, Object> person = Map.of(
			"Studiestartsstod", Map.of("Arenden", Map.of("Arende", Map.of(
				"klartext", "Studiestartsstöd",
				"UtbetalningsPlan", Map.of("Utbetalning", Map.of("utbetdatum", "20260601", "totbelopp", "3000"))))),
			"Omstallningsstudiestod", Map.of("Arenden", Map.of("Arende", Map.of(
				"klartext", "Omställningsstudiestöd",
				"UtbetalningsPlan", Map.of("Utbetalning", Map.of("utbetdatum", "20260601", "totbelopp", "4000"))))));

		final var incomes = SsbtekIncomeExtractor.extract(personBasis(person), APPLICANT);

		// "Omställningsstudiestöd" matches Decision_inkomstTroskel's threshold row verbatim (Rule_ral_omstallning);
		// "Studiestartsstöd" has no rålista row yet, which is fine - it falls through to the catch-all warning
		assertThat(incomes).extracting(SsbtekIncome::benefit)
			.containsExactlyInAnyOrder("Studiestartsstöd", "Omställningsstudiestöd");
	}
}
