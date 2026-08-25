package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;

/**
 * A warning that a benefit's net income changed between the comparison and control periods by more than the threshold
 * returned from {@code Decision_inkomstTroskel}.
 */
public record ChangeWarning(
	String benefit,
	BigDecimal changePercent,
	BigDecimal comparisonSum,
	BigDecimal controlSum) {
}
