package se.sundsvall.operaton.workers.financialaid.rules;

/** Which household member an SSBTEK income belongs to. */
public enum ApplicantRole {
	APPLICANT,
	CO_APPLICANT,
	/** A household child; {@link SsbtekIncome#partyId()} says which one. */
	CHILD
}
