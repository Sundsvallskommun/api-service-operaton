package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One responding organisation's answer within an SSBTEK agency response, before it has been classified.
 * <p>
 * SSBTEK asks several organisations on our behalf and each answers for itself - the SO response repeats
 * {@code Arbetsloshetsersattning} once per a-kassa - so answer quality is always <em>per organisation</em>, never per
 * person. One a-kassa can answer properly while another cannot.
 * <p>
 * {@code status} is the raw {@code StatusSvarandeOrganisation}. The contract declares it {@code xs:string} with no
 * enumeration and no annotation, so its codes are undefined by the contract itself and are deliberately <em>not</em>
 * interpreted here - {@code Decision_ssbtekSvarKvalitet} does that, so the reading stays editable at runtime. See
 * {@code vof-ekonomiskt-bistand/architecture/ssbtek-svarskvalitet.md}.
 * <p>
 * {@code applicationInfoPresent} records whether the organisation said anything at all about an application
 * ({@code AnsoktOmErsattning} is {@code 0..1}), which is a different thing from saying there is none.
 * <p>
 * {@code paymentsPresent} is what makes the classification usable without the code list: an organisation that returned
 * payments has evidently answered, whatever its status code says. That inference needs nobody's confirmation, and it
 * keeps the "kunde inte kontrolleras" flag off every applicant who actually draws a-kassa.
 */
public record AgencyAnswer(
	@JsonProperty("myndighet") String agency,
	@JsonProperty("organisation") String organisation,
	@JsonProperty("status") String status,
	@JsonProperty("ansokningsuppgiftFinns") boolean applicationInfoPresent,
	@JsonProperty("utbetalningarFinns") boolean paymentsPresent) {
}
