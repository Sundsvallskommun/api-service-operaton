package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;

/**
 * The five FK payment shapes that decide whether our day counting is right, pinned as executable expectations.
 * <p>
 * <b>These are ASSUMED values.</b> We have no contact at FK and no verified populated test case, so the numbers here
 * are invented; only the payload <em>structure</em> is grounded, in the LEFI shape already flowing through the
 * WireMock stubs. The point is not that the numbers are real - it is that each assumption is written down somewhere
 * that fails loudly when the behaviour changes, so that correcting them later is mechanical rather than archaeological.
 * <p>
 * The same six payments are installed as a WireMock fixture for end-to-end use:
 * {@code vof-ekonomiskt-bistand/deploy/fixtures/ssbtek/2026-10/fk-daycount-assumed.json}, synthetic identity
 * {@code 199001015544}. When real FK values arrive, correct both together.
 *
 * @see <a href="https://sundsvall.se">architecture/ssbtek-svarskvalitet.md in the sprint HQ folder</a>
 */
class SsbtekFkDayCountAssumptionTest {

	private static final YearMonth APPLICATION_MONTH = YearMonth.of(2026, 10);

	private static Map<String, Object> payment(final String benefit, final String datum, final String fran,
		final String till, final String net, final List<Object> details) {
		return Map.of(
			"formansfamilj", Map.of("id", "X", "beskrivning", benefit),
			"datum", datum,
			"period", Map.of("fran", fran, "till", till),
			"nettobelopp", Map.of("summa", net, "valuta", "SEK"),
			"utbetalningsdetalj", details);
	}

	private static Map<String, Object> detail(final String amountType, final Object days) {
		return Map.of(
			"forman", Map.of("beskrivning", "Föräldrapenning"),
			"beloppstyp", Map.of("beskrivning", amountType),
			"dagar", days);
	}

	private static List<SsbtekIncome> extract(final Map<String, Object>... payments) {
		// FK's effectuated payments live under formansinformation.utbetalningsuppgift, not the top-level
		// "utbetalningar" - that key is Pensionsmyndighetens, per the fix for the bug this repo's javadoc used to describe.
		return SsbtekIncomeExtractor.extract(
			Map.of("fk", Map.of("formansinformation", Map.of("utbetalningsuppgift", List.of((Object[]) payments)))), APPLICANT);
	}

	/**
	 * Case 1. A payment split over several detail rows has no single sub-benefit, amount type or day count, so none is
	 * lifted. The assumption being pinned is that the three rows are <em>not</em> summed into 30 days: they are an
	 * amount and its deductions, describing the same days, and summing them would triple the day count.
	 */
	@Test
	void aPaymentWithSeveralDetailRowsContributesNoDayCountRatherThanTheirSum() {
		final var income = extract(payment("Föräldrapenning", "2026-09-25", "2026-09-01", "2026-09-30", "8400",
			List.of(detail("Belopp", 10), detail("Preliminärskatt", 10), detail("Avdrag Soc", 10)))).getFirst();

		assertThat(income.days()).isNull();
		assertThat(income.subBenefit()).isNull();
		assertThat(income.amountType()).isNull();
		// The amount stays the payment-level net; summing detail rows would change what is transferred.
		assertThat(income.netAmount()).isEqualByComparingTo("8400");
	}

	/** Case 2. Half a day survives. This is the regression guard for the int truncation fixed 2026-09-17. */
	@Test
	void aHalfDayIsCarriedAsAHalfDay() {
		final var income = extract(payment("Föräldrapenning", "2026-09-18", "2026-09-15", "2026-09-15", "420",
			List.of(detail("Belopp", 0.5)))).getFirst();

		assertThat(income.days()).isEqualByComparingTo("0.5");
		assertThat(income.days()).isNotEqualByComparingTo(BigDecimal.ZERO);
	}

	/** Case 3. A single detail row does yield its amount type, which is what the rålista keys off. */
	@Test
	void aSingleDetailRowYieldsItsAmountTypeAndDayCount() {
		final var income = extract(payment("Bostadsbidrag", "2026-09-25", "2026-09-01", "2026-09-30", "3900",
			List.of(detail("Avdrag Soc", 30)))).getFirst();

		assertThat(income.amountType()).isEqualTo("Avdrag Soc");
		assertThat(income.days()).isEqualByComparingTo("30");
	}

	/**
	 * Case 4. A period straddling a month boundary is attributed to the month it starts in, so an August-starting
	 * period lands in the comparison period of an October application rather than the control period.
	 */
	@Test
	void aPeriodStraddlingAMonthBoundaryIsAttributedToTheMonthItStartsIn() {
		final var income = extract(payment("Föräldrapenning", "2026-09-10", "2026-08-25", "2026-09-05", "4620",
			List.of(detail("Belopp", 11)))).getFirst();

		assertThat(income.attributionDate()).isEqualTo(LocalDate.of(2026, Month.AUGUST, 25));

		final var periods = SsbtekPeriods.forApplicationMonth(APPLICATION_MONTH);
		assertThat(periods.isInComparisonPeriod(income.attributionDate())).isTrue();
		assertThat(periods.isInControlPeriod(income.attributionDate())).isFalse();
	}

	/**
	 * Case 5. Two periods with a gap between them (11-19 September) are both extracted and the gap is currently
	 * <em>not</em> flagged - the period checks in {@code rakel-eb-periodkontroll} are published but not wired in. This
	 * test exists to say that the silence is known, so that whoever wires them up sees the case already described.
	 */
	@Test
	void aGapBetweenTwoPeriodsIsCarriedButNotYetFlagged() {
		final var incomes = extract(
			payment("Föräldrapenning", "2026-09-14", "2026-09-01", "2026-09-10", "4200", List.of(detail("Belopp", 10))),
			payment("Föräldrapenning", "2026-09-30", "2026-09-20", "2026-09-30", "4620", List.of(detail("Belopp", 11))));

		assertThat(incomes).hasSize(2);
		assertThat(incomes.getFirst().periodTo()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 10));
		assertThat(incomes.getLast().periodFrom()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 20));
		assertThat(incomes).allSatisfy(income -> assertThat(income.days()).isNotNull());
	}
}
