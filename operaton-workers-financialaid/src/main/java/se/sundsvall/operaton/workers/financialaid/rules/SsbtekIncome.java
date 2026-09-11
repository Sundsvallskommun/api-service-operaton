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

	/** The payment-date-only shape, for callers that have no period or day information. */
	public SsbtekIncome(final String benefit, final String subBenefit, final String amountType,
		final BigDecimal netAmount, final LocalDate period, final ApplicantRole role) {
		this(benefit, subBenefit, amountType, netAmount, period, null, null, null, role);
	}
}
