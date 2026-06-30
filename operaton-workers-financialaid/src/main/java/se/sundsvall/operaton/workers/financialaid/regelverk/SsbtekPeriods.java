package se.sundsvall.operaton.workers.financialaid.regelverk;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The three SSBTEK rule periods, derived from the ansökningsmånad (ssbtek-regelverk.txt): ansökningsperiod (the applied
 * month), kontrollperiod (month before — primary income source), jämförelseperiod (two months before — fills gaps and
 * is compared for change warnings).
 */
public record SsbtekPeriods(YearMonth ansokningsperiod, YearMonth kontrollperiod, YearMonth jamforelseperiod) {

	public static SsbtekPeriods forApplicationMonth(final YearMonth applicationMonth) {
		return new SsbtekPeriods(applicationMonth, applicationMonth.minusMonths(1), applicationMonth.minusMonths(2));
	}

	public boolean isInKontrollperiod(final LocalDate date) {
		return inMonth(date, kontrollperiod);
	}

	public boolean isInJamforelseperiod(final LocalDate date) {
		return inMonth(date, jamforelseperiod);
	}

	private static boolean inMonth(final LocalDate date, final YearMonth month) {
		return (date != null) && YearMonth.from(date).equals(month);
	}
}
