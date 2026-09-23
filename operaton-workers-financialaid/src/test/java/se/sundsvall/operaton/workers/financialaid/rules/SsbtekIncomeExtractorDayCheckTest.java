package se.sundsvall.operaton.workers.financialaid.rules;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The AF/FK facts behind caremanagement's dagersättning day check. What matters most is the tri-state: an agency that
 * was not read must come out {@code null}, never as "answered with nothing" - the latter can open the gate and raise a
 * warning on data we never had.
 */
class SsbtekIncomeExtractorDayCheckTest {

	private static final Map<String, Object> AGENCY_ERROR = Map.of("error", Map.of("kalla", "AF", "felkod", "1", "felmeddelande", "Tekniskt fel"));

	private static Map<String, Object> af(final Object beslut) {
		return Map.of("Svar", Map.of("Schemaversion", "4.0", "BeslutInfo", Map.of("EkonomiskaBeslut", Map.of("Beslut", beslut))));
	}

	private static Map<String, Object> fk(final Object programjobdagar) {
		return Map.of("formansinformation", Map.of("utbetalningsuppgift", List.of(), "programjobdagar", programjobdagar));
	}

	@Test
	void readsTheDecisionPeriodsAndTheConsumedDays() {
		final var facts = SsbtekIncomeExtractor.extractDayCheckFacts(Map.of(
			"af", af(List.of(
				Map.of("ErsattningTyp", "AKT", "BeslutFrom", "2026-08-01+02:00", "BeslutTom", "2026-12-31+01:00"),
				Map.of("ErsattningTyp", "AKT", "BeslutFrom", "2027-01-01"))),
			"fk", fk(List.of(
				Map.of("antalForbrukade", 212, "harForbrukatMaxAntal", false),
				Map.of("antalForbrukade", 300, "harForbrukatMaxAntal", false)))));

		assertThat(facts.economicDecisionPeriods()).containsExactly(
			new DayCheckFacts.DecisionPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31)),
			new DayCheckFacts.DecisionPeriod(LocalDate.of(2027, 1, 1), null));
		assertThat(facts.consumedDays()).isEqualTo(300);
		assertThat(facts.allDaysConsumed()).isFalse();
	}

	@Test
	void aSingleDecisionAndAStringFlagAreReadLikeLists() {
		final var facts = SsbtekIncomeExtractor.extractDayCheckFacts(Map.of(
			"af", af(Map.of("BeslutFrom", "2026-08-01", "BeslutTom", "2026-08-31")),
			"fk", fk(Map.of("antalForbrukade", "450", "harForbrukatMaxAntal", "true"))));

		assertThat(facts.economicDecisionPeriods()).containsExactly(
			new DayCheckFacts.DecisionPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));
		assertThat(facts.consumedDays()).isEqualTo(450);
		assertThat(facts.allDaysConsumed()).isTrue();
	}

	@Test
	void anAnswerWithoutDecisionsOrProgramDaysIsAnsweredNotUnread() {
		final var facts = SsbtekIncomeExtractor.extractDayCheckFacts(Map.of(
			"af", Map.of("Svar", Map.of("Schemaversion", "4.0", "Akassetillhorighet", "Test ak")),
			"fk", Map.of("formansinformation", Map.of("utbetalningsuppgift", List.of()))));

		assertThat(facts.economicDecisionPeriods()).isEmpty();
		assertThat(facts.consumedDays()).isNull();
		assertThat(facts.allDaysConsumed()).isFalse();
	}

	@Test
	void anAgencyErrorIsUnread() {
		final var facts = SsbtekIncomeExtractor.extractDayCheckFacts(Map.of("af", AGENCY_ERROR, "fk", AGENCY_ERROR));

		assertThat(facts).isEqualTo(new DayCheckFacts(null, null, null));
	}

	@Test
	void anAbsentAgencyOrAnAfBlockWithoutSvarIsUnread() {
		assertThat(SsbtekIncomeExtractor.extractDayCheckFacts(Map.of())).isEqualTo(new DayCheckFacts(null, null, null));
		assertThat(SsbtekIncomeExtractor.extractDayCheckFacts(Map.of("af", Map.of("Other", "x"))).economicDecisionPeriods()).isNull();
		assertThat(SsbtekIncomeExtractor.extractDayCheckFacts(null)).isEqualTo(new DayCheckFacts(null, null, null));
	}

	@Test
	void decisionsWithoutAnyDateAndUnparsableCountsAreSkipped() {
		final var beslutWithNullDates = new HashMap<String, Object>();
		beslutWithNullDates.put("BeslutFrom", null);
		beslutWithNullDates.put("BeslutTom", null);

		final var facts = SsbtekIncomeExtractor.extractDayCheckFacts(Map.of(
			"af", af(List.of(beslutWithNullDates)),
			"fk", fk(List.of(Map.of("antalForbrukade", "okänt")))));

		assertThat(facts.economicDecisionPeriods()).isEmpty();
		assertThat(facts.consumedDays()).isNull();
		assertThat(facts.allDaysConsumed()).isFalse();
	}
}
