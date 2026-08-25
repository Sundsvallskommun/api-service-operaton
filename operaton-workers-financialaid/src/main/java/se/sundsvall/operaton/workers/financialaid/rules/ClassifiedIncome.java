package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An SSBTEK income after the verdict from the {@code Decision_inkomstRalista} DMN: the action to take, the target
 * calculation category, whether to flag it, and the human-readable rule note. The serialized JSON keys stay Swedish
 * because caremanagement consumes the {@code classifiedIncomes} contract.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	@JsonProperty("atgard") String action,
	@JsonProperty("normberakning") String calculation,
	@JsonProperty("varning") boolean warning,
	@JsonProperty("regel") String rule) {
}
