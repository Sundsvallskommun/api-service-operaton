package se.sundsvall.operaton.workers.financialaid.rules;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An SSBTEK income after the verdict from the {@code Decision_inkomstRalista} DMN: the action to take, the target
 * calculation category, whether to flag it, and the human-readable rule note. The serialized JSON keys stay Swedish
 * because caremanagement consumes the {@code classifiedIncomes} contract.
 * <p>
 * {@code fromComparisonPeriod} marks the incomes picked up from the comparison period rather than the control period.
 * The regelverk only wants those "som inte togs med månaden innan", and whether they were is answered by the previous
 * month's calculation in Lifecare - which caremanagement can read and the engine cannot. So the engine says where the
 * income came from and caremanagement makes the final call.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	@JsonProperty("atgard") String action,
	@JsonProperty("normberakning") String calculation,
	@JsonProperty("varning") boolean warning,
	@JsonProperty("regel") String rule,
	@JsonProperty("jamforelseperiod") boolean fromComparisonPeriod) {

	/** A control-period income - the ordinary case, never filtered against the previous month. */
	public ClassifiedIncome(final SsbtekIncome income, final String action, final String calculation,
		final boolean warning, final String rule) {
		this(income, action, calculation, warning, rule, false);
	}
}
