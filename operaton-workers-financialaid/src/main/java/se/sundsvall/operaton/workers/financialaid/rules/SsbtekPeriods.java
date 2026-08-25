package se.sundsvall.operaton.workers.financialaid.rules;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The three SSBTEK rule periods, derived from the application month: application period, control period (the previous
 * month and primary income source), and comparison period (two months back, used to fill gaps and detect changes).
 */
public record SsbtekPeriods(YearMonth applicationPeriod, YearMonth controlPeriod, YearMonth comparisonPeriod) {

	public static SsbtekPeriods forApplicationMonth(final YearMonth applicationMonth) {
		return new SsbtekPeriods(applicationMonth, applicationMonth.minusMonths(1), applicationMonth.minusMonths(2));
	}

	public boolean isInControlPeriod(final LocalDate date) {
		return inMonth(date, controlPeriod);
	}

	public boolean isInComparisonPeriod(final LocalDate date) {
		return inMonth(date, comparisonPeriod);
	}

	private static boolean inMonth(final LocalDate date, final YearMonth month) {
		return (date != null) && YearMonth.from(date).equals(month);
	}
}
