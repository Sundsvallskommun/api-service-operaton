package se.sundsvall.operaton.workers.financialaid.regelverk;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An SSBTEK income after the regelverk verdict from the {@code Decision_inkomstRalista} DMN: the action to take, the
 * target normberäkning category, whether to flag it, and the human-readable rule note. The serialized JSON keys stay
 * Swedish — the {@code classifiedIncomes} contract caremanagement consumes — mapped onto English record components via
 * {@link JsonProperty}.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	@JsonProperty("atgard") String action,
	@JsonProperty("normberakning") String calculation,
	@JsonProperty("varning") boolean warning,
	@JsonProperty("regel") String rule) {
}
