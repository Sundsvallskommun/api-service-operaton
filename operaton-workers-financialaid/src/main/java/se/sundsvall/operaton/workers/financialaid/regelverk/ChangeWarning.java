package se.sundsvall.operaton.workers.financialaid.regelverk;

import java.math.BigDecimal;

/**
 * A warning that a förmån's net income changed between the jämförelse- and kontrollperiod by more than the threshold
 * the
 * {@code Decision_inkomstTroskel} DMN returned for it.
 */
public record ChangeWarning(
	String forman,
	BigDecimal changePercent,
	BigDecimal jamforelseSum,
	BigDecimal kontrollSum) {
}
