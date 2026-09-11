package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One normalised SSBTEK income, parsed from the api-service-financial-aid basis. The rule DMNs key off benefit,
 * sub-benefit, and amount type; {@code netAmount} and {@code period} drive period selection and change detection. The
 * serialized JSON keys stay aligned with the downstream Swedish contract.
 */
public record SsbtekIncome(
	@JsonProperty("forman") String benefit,
	@JsonProperty("delforman") String subBenefit,
	@JsonProperty("beloppstyp") String amountType,
	BigDecimal netAmount,
	LocalDate period,
	@JsonProperty("periodFran") LocalDate periodFrom,
	@JsonProperty("periodTill") LocalDate periodTo,
	@JsonProperty("dagar") Integer days,
	ApplicantRole role) {

	/**
	 * The date this income is attributed to when placing it in a rule period.
	 * <p>
	 * The period the payment <em>covers</em> wins over the date it was paid: a payment made on 2 October for September
	 * belongs to September. Verksamheten confirmed this reading on 2026-09-11 - "inkomsterna som ska tas med är för
	 * kontrollperioden". Payments whose covered period spans two months are attributed to the month it starts in.
	 * <p>
	 * Falls back to the payment date when the payload carries no period, which is the case for every payment that is
	 * split over several detail rows and for agencies that send no period at all.
	 */
	public LocalDate attributionDate() {
		return (periodFrom != null) ? periodFrom : period;
	}

	/** The payment-date-only shape, for callers that have no period or day information. */
	public SsbtekIncome(final String benefit, final String subBenefit, final String amountType,
		final BigDecimal netAmount, final LocalDate period, final ApplicantRole role) {
		this(benefit, subBenefit, amountType, netAmount, period, null, null, null, role);
	}
}
