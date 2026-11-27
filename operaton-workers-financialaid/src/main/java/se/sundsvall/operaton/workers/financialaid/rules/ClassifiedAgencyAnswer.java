package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An {@link AgencyAnswer} after the verdict from the {@code Decision_ssbtekSvarKvalitet} DMN.
 * <p>
 * {@code quality} is one of {@code SVARAT} (a usable answer), {@code INGET_HOS_OSS} (the organisation stated it has
 * nothing for this person) or {@code EJ_KONTROLLERBAR} (anything else). The table's catch-all row yields
 * {@code EJ_KONTROLLERBAR}, so an unknown status code degrades to "cannot be checked" rather than being read as a
 * statement about the sökande - absence of data is never evidence of absence of income.
 * <p>
 * {@code rule} is the DMN's human-readable note, shown to the handläggare as the reason the check could not be made.
 */
public record ClassifiedAgencyAnswer(
	AgencyAnswer answer,
	@JsonProperty("kvalitet") String quality,
	@JsonProperty("regel") String rule) {

	static final String QUALITY_ANSWERED = "SVARAT";
	static final String QUALITY_NOTHING_HERE = "INGET_HOS_OSS";
	static final String QUALITY_UNVERIFIABLE = "EJ_KONTROLLERBAR";

	/** Whether this answer must be surfaced to the handläggare as something the engine could not verify. */
	public boolean unverifiable() {
		return QUALITY_UNVERIFIABLE.equals(quality);
	}
}
