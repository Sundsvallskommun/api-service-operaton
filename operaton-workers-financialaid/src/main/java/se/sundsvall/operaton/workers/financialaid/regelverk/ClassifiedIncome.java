package se.sundsvall.operaton.workers.financialaid.regelverk;

/**
 * An SSBTEK income after the regelverk verdict from the {@code Decision_inkomstRalista} DMN: the action to take, the
 * target normberäkning category, whether to flag it, and the human-readable rule note.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	String atgard,
	String normberakning,
	boolean varning,
	String regel) {
}
