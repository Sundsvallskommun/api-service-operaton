package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One normalised SSBTEK income, parsed from the api-service-financial-aid basis. The rule DMNs key off benefit,
 * sub-benefit, and amount type; {@code netAmount} and {@code period} drive period selection and change detection. The
 * serialized JSON keys stay aligned with the downstream Swedish contract.
 * <p>
 * {@code days} is a decimal, not a whole number: the SO contract declares {@code Ersattningsdagar} as
 * {@code xs:decimal} and FK sends partial parental-benefit days, so quarter and half days are native to both formats.
 * Truncating them to an int silently turned half a day into none, before any rule got to decide what half a day means.
 */
public record SsbtekIncome(
	@JsonProperty("forman") String benefit,
	@JsonProperty("delforman") String subBenefit,
	@JsonProperty("beloppstyp") String amountType,
	BigDecimal netAmount,
	LocalDate period,
	@JsonProperty("periodFran") LocalDate periodFrom,
	@JsonProperty("periodTill") LocalDate periodTo,
	@JsonProperty("dagar") BigDecimal days,
	ApplicantRole role) {

	/**
	 * The date this income is attributed to when placing it in a rule period: the date it was <em>paid</em>.
	 * <p>
	 * A payment made on 2 May belongs to May, whatever month it covers. Verksamheten decided this on 2026-09-21 -
	 * "det är utbetalningsdatumet som styr vilken inkomst som ska tas med till normberäkningen" - reversing the
	 * opposite reading they had confirmed on 2026-09-11 and which this method carried until now (commit 8f6f2d2,
	 * "place an income in the period it covers, not the day it was paid").
	 * <p>
	 * The reversal is about which of the two <em>wins</em>, not about discarding the period: CSN schedules a payment
	 * with {@code utbetdatum = "0"} - "not booked yet" - and only a week-derived period to place it by. Dropping back
	 * to the covered period there keeps such a payment in a rule period instead of silently losing it, which is what
	 * reading {@code period()} alone would do. So: the payment date decides whenever there is one.
	 * <p>
	 * Known conflict, verksamhetens to resolve: the aktivitetsstöd rule in the regelverk still reads "månaden som
	 * ersättningen avser", which is the reading this reversal drops.
	 */
	public LocalDate attributionDate() {
		if (period != null) {
			return period;
		}
		return periodFrom;
	}

	/** The payment-date-only shape, for callers that have no period or day information. */
	public SsbtekIncome(final String benefit, final String subBenefit, final String amountType,
		final BigDecimal netAmount, final LocalDate period, final ApplicantRole role) {
		this(benefit, subBenefit, amountType, netAmount, period, null, null, null, role);
	}
}
