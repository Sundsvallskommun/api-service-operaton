package se.sundsvall.operaton.workers.financialaid.rules;

import java.util.List;

/**
 * The outcome of evaluating the SSBTEK income rules over a household's incomes: the transferable incomes with their
 * per-income verdict, plus the benefit-level change warnings.
 */
public record IncomeRulesResult(
	List<ClassifiedIncome> classified,
	List<ChangeWarning> changeWarnings,
	List<ClassifiedAgencyAnswer> answers) {

	/**
	 * The income-only shape, for callers with no agency answers to classify. {@code answers} is then empty, which reads
	 * as "nothing was checked", not as "everything answered" - a caller must not infer quality from an empty list.
	 */
	public IncomeRulesResult(final List<ClassifiedIncome> classified, final List<ChangeWarning> changeWarnings) {
		this(classified, changeWarnings, List.of());
	}
}
