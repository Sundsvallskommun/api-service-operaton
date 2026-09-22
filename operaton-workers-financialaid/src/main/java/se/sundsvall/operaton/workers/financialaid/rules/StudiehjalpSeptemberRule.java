package se.sundsvall.operaton.workers.financialaid.rules;

import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncomeExtractor.CSN_STUDIEHJALP;

/**
 * Verksamhetens säsongsregel för studiehjälp: i ansökan för oktober, kontrollera om det fanns studiehjälp utbetalat i
 * juni. Fanns inget - gör ingenting. Fanns det - kontrollera om det finns utbetalning i slutet av september. Finns den
 * - gör ingenting. Saknas den - generera varning.
 *
 * <p>
 * The rule catches a gymnasieelev who stopped studying over the summer: studiehjälp runs through June, pauses for the
 * holiday and resumes in September. June paid but September missing is the signal that the young person may no longer
 * be entitled - while the household's normberäkning has been counting on the money.
 * </p>
 *
 * <p>
 * Two readings this class makes explicit, because the regelverk leaves them open:
 * </p>
 * <ul>
 * <li><strong>Slutet av september is read as September.</strong> CSN pays studiehjälp at the end of the month, so the
 * phrase describes when the payment lands rather than adding a day-of-month condition. Narrowing it to the last week
 * would invent precision the regelverk does not have, and would miss a payment CSN booked early.</li>
 * <li><strong>A planned payment counts.</strong> Reading the CSN tab at all is only worthwhile because the September
 * payment syns alldeles för sent i månaden (verksamheten 2026-09-22) - so a payment that is scheduled but not yet made
 * is exactly what the handläggare needs to see. {@link SsbtekIncomeExtractor} already carries both Utbetald and
 * Planerad, and a not-yet-booked payment is dated from its benefit weeks.</li>
 * </ul>
 *
 * <p>
 * The rule answers presence, never amount. That matters: which CSN field is the month's studiehjälp amount is still
 * open with verksamheten, and nothing here depends on the answer.
 * </p>
 */
public final class StudiehjalpSeptemberRule {

	/** The month an application must be for; outside it the rule does not apply at all. */
	static final Month APPLICATION_MONTH = Month.OCTOBER;
	static final Month PAID_MONTH = Month.JUNE;
	static final Month EXPECTED_MONTH = Month.SEPTEMBER;

	static final String WARNING = "Studiehjälp utbetalades i juni men utbetalning saknas i september – kontrollera om ungdom ska ha studiehjälp";

	private StudiehjalpSeptemberRule() {}

	/**
	 * Whether the rule applies to this application month at all - the caller uses it to avoid the extra SSBTEK read
	 * eleven months of the year.
	 */
	public static boolean appliesTo(final YearMonth applicationMonth) {
		return (applicationMonth != null) && (applicationMonth.getMonth() == APPLICATION_MONTH);
	}

	/**
	 * Evaluate the rule against the studiehjälp payments in a basis that reaches back to June.
	 *
	 * @param  applicationMonth the month applied for
	 * @param  incomes          the incomes extracted from the wider-window SSBTEK answer
	 * @return                  the warning when June paid and September did not, otherwise nothing
	 */
	public static Optional<String> evaluate(final YearMonth applicationMonth, final List<SsbtekIncome> incomes) {
		if (!appliesTo(applicationMonth)) {
			return empty();
		}

		final var studiehjalp = studiehjalpOf(incomes);
		if (!paidIn(studiehjalp, applicationMonth.getYear(), PAID_MONTH)) {
			return empty(); // no studiehjälp before the summer - nothing to have stopped
		}
		if (paidIn(studiehjalp, applicationMonth.getYear(), EXPECTED_MONTH)) {
			return empty(); // it resumed as it should
		}
		return of(WARNING);
	}

	/** The window the caller must ask SSBTEK for: the first of June through the end of the application month. */
	public static YearMonth readFrom(final YearMonth applicationMonth) {
		return YearMonth.of(applicationMonth.getYear(), PAID_MONTH);
	}

	private static List<SsbtekIncome> studiehjalpOf(final List<SsbtekIncome> incomes) {
		return Optional.ofNullable(incomes).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(income -> CSN_STUDIEHJALP.equalsIgnoreCase(income.benefit()))
			.toList();
	}

	private static boolean paidIn(final List<SsbtekIncome> studiehjalp, final int year, final Month month) {
		return studiehjalp.stream()
			.map(SsbtekIncome::attributionDate)
			.filter(Objects::nonNull)
			.anyMatch(date -> (date.getYear() == year) && (date.getMonth() == month));
	}
}
