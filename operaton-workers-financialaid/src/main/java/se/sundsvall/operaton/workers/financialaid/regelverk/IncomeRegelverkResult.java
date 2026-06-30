package se.sundsvall.operaton.workers.financialaid.regelverk;

import java.util.List;

/**
 * The outcome of evaluating the SSBTEK regelverk over a household's incomes: the transferable incomes with their
 * per-income verdict, plus the förmån-level change warnings.
 */
public record IncomeRegelverkResult(
	List<ClassifiedIncome> classified,
	List<ChangeWarning> changeWarnings) {
}
