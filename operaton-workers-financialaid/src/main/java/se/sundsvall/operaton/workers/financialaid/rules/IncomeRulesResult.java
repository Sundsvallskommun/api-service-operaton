package se.sundsvall.operaton.workers.financialaid.rules;

import java.util.List;

/**
 * The outcome of evaluating the SSBTEK income rules over a household's incomes: the transferable incomes with their
 * per-income verdict, plus the benefit-level change warnings.
 */
public record IncomeRulesResult(
	List<ClassifiedIncome> classified,
	List<ChangeWarning> changeWarnings) {
}
