package se.sundsvall.operaton.workers.financialaid.regelverk;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One normalised SSBTEK income, parsed from the api-service-financial-aid basis. The regelverk (rålista + thresholds)
 * keys off förmån/delförmån/beloppstyp; {@code netAmount} + {@code period} drive period selection and change detection.
 */
public record SsbtekIncome(
	String forman,
	String delforman,
	String beloppstyp,
	BigDecimal netAmount,
	LocalDate period,
	ApplicantRole role) {
}
