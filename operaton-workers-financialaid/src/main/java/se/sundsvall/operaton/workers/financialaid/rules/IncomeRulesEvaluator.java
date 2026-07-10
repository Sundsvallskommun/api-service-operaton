package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.operaton.bpm.engine.DecisionService;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

/**
 * Evaluates the SSBTEK income rules for a household. The engine applies the period transfer rule first: all control
 * period incomes are included, and comparison period incomes are added only when the same benefit has no control period
 * income. Runtime-published DMNs then classify each transferable income and provide the change-warning threshold per
 * benefit, keeping the rules editable in the modeler without code changes.
 */
@Component
public class IncomeRulesEvaluator {

	static final String INCOME_ALLOW_LIST_DECISION_KEY = "Decision_inkomstRalista";
	static final String INCOME_THRESHOLD_DECISION_KEY = "Decision_inkomstTroskel";

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final BigDecimal DEFAULT_THRESHOLD_PERCENT = BigDecimal.valueOf(12);

	private final DecisionService decisionService;

	public IncomeRulesEvaluator(final DecisionService decisionService) {
		this.decisionService = decisionService;
	}

	/**
	 * Evaluate the income rules over the household's incomes for the application month.
	 *
	 * @param  incomes          the normalised SSBTEK incomes; may be {@code null}
	 * @param  applicationMonth the month the application concerns
	 * @return                  the transferable incomes with their per-income verdict, plus benefit-level change warnings
	 */
	public IncomeRulesResult evaluate(final List<SsbtekIncome> incomes, final YearMonth applicationMonth) {
		final var present = ofNullable(incomes).orElseGet(List::of).stream().filter(Objects::nonNull).toList();
		final var periods = SsbtekPeriods.forApplicationMonth(applicationMonth);

		final var classified = selectTransferable(present, periods).stream()
			.map(this::classify)
			.toList();

		return new IncomeRulesResult(classified, detectChanges(present, periods));
	}

	/** Control period incomes plus comparison period incomes whose benefit has no control period income. */
	private static List<SsbtekIncome> selectTransferable(final List<SsbtekIncome> present, final SsbtekPeriods periods) {
		final var controlPeriodIncomes = present.stream().filter(income -> periods.isInControlPeriod(income.period())).toList();
		final Set<String> controlPeriodBenefits = controlPeriodIncomes.stream().map(income -> normalize(income.benefit())).collect(toSet());
		final var comparisonPeriodFallbackIncomes = present.stream()
			.filter(income -> periods.isInComparisonPeriod(income.period()))
			.filter(income -> !controlPeriodBenefits.contains(normalize(income.benefit())))
			.toList();
		return concat(controlPeriodIncomes.stream(), comparisonPeriodFallbackIncomes.stream()).toList();
	}

	/** The per-income allow-list verdict from {@code Decision_inkomstRalista}. */
	private ClassifiedIncome classify(final SsbtekIncome income) {
		final var row = evaluateFirst(INCOME_ALLOW_LIST_DECISION_KEY, Map.of(
			"forman", nullToEmpty(income.benefit()),
			"delforman", nullToEmpty(income.subBenefit()),
			"beloppstyp", nullToEmpty(income.amountType())));
		return new ClassifiedIncome(income, str(row.get("atgard")), str(row.get("normberakning")),
			Boolean.TRUE.equals(row.get("varning")), str(row.get("regel")));
	}

	/** Per-benefit change warnings: comparison vs control net sum, flagged when the change exceeds the DMN threshold. */
	private List<ChangeWarning> detectChanges(final List<SsbtekIncome> present, final SsbtekPeriods periods) {
		final var controlSums = sumByBenefit(present.stream().filter(income -> periods.isInControlPeriod(income.period())).toList());
		final var comparisonPeriodIncomes = present.stream().filter(income -> periods.isInComparisonPeriod(income.period())).toList();
		final var displayNames = comparisonPeriodIncomes.stream().collect(toMap(income -> normalize(income.benefit()), SsbtekIncome::benefit, (first, second) -> first));

		return sumByBenefit(comparisonPeriodIncomes).entrySet().stream()
			.filter(entry -> entry.getValue().signum() != 0)
			.map(entry -> {
				final var comparisonSum = entry.getValue();
				final var controlSum = controlSums.getOrDefault(entry.getKey(), BigDecimal.ZERO);
				final var changePercent = controlSum.subtract(comparisonSum).multiply(HUNDRED)
					.divide(comparisonSum.abs(), 0, RoundingMode.HALF_UP);
				return new ChangeWarning(displayNames.get(entry.getKey()), changePercent, comparisonSum, controlSum);
			})
			.filter(warning -> warning.changePercent().abs().compareTo(thresholdFor(warning.benefit())) > 0)
			.toList();
	}

	private BigDecimal thresholdFor(final String benefit) {
		return ofNullable(evaluateFirst(INCOME_THRESHOLD_DECISION_KEY, Map.of("forman", nullToEmpty(benefit))).get("troskelProcent"))
			.map(value -> new BigDecimal(value.toString()))
			.orElse(DEFAULT_THRESHOLD_PERCENT);
	}

	private Map<String, Object> evaluateFirst(final String decisionKey, final Map<String, Object> variables) {
		final var rows = decisionService.evaluateDecisionByKey(decisionKey).variables(variables).evaluate().getResultList();
		return rows.isEmpty() ? Map.of() : rows.getFirst();
	}

	private static Map<String, BigDecimal> sumByBenefit(final List<SsbtekIncome> incomes) {
		return incomes.stream()
			.filter(income -> income.netAmount() != null)
			.collect(groupingBy(income -> normalize(income.benefit()),
				mapping(SsbtekIncome::netAmount, reducing(BigDecimal.ZERO, BigDecimal::add))));
	}

	private static String normalize(final String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}

	private static String nullToEmpty(final String value) {
		return value == null ? "" : value;
	}

	private static String str(final Object value) {
		return value == null ? null : value.toString();
	}
}
