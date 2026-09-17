package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;

/**
 * A warning that a benefit's net income differs between the comparison and control periods by more than the threshold
 * returned from {@code Decision_inkomstTroskel}.
 * <p>
 * {@code changePercent} is {@code null} when there is no comparison sum to express the change as a share of - the case
 * for a benefit that only appears in the control period, which verksamhetens exact comparison (threshold 0) still warns
 * about. Consumers then have the two sums to show instead.
 * <p>
 * {@code rule} is verksamhetens warning text from the DMN, and is {@code null} when the deployed table carries no
 * {@code regel} output.
 */
public record ChangeWarning(
	String benefit,
	BigDecimal changePercent,
	BigDecimal comparisonSum,
	BigDecimal controlSum,
	String rule) {
}
