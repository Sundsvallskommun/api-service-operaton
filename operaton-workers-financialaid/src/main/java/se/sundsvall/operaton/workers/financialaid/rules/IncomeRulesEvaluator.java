package se.sundsvall.operaton.workers.financialaid.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.operaton.bpm.engine.DecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOG = LoggerFactory.getLogger(IncomeRulesEvaluator.class);

	static final String INCOME_ALLOW_LIST_DECISION_KEY = "Decision_inkomstRalista";
	static final String INCOME_THRESHOLD_DECISION_KEY = "Decision_inkomstTroskel";
	static final String ANSWER_QUALITY_DECISION_KEY = "Decision_ssbtekSvarKvalitet";

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
		return evaluate(incomes, List.of(), applicationMonth);
	}

	/**
	 * Evaluate the income rules, and classify how far each responding organisation's answer can be trusted.
	 *
	 * @param answers the per-organisation answers from {@link SsbtekIncomeExtractor#extractAnswers}; may be {@code null}
	 */
	public IncomeRulesResult evaluate(final List<SsbtekIncome> incomes, final List<AgencyAnswer> answers,
		final YearMonth applicationMonth) {
		final var present = ofNullable(incomes).orElseGet(List::of).stream().filter(Objects::nonNull).toList();
		final var periods = SsbtekPeriods.forApplicationMonth(applicationMonth);

		final var transferable = selectTransferable(present, periods);
		final var classified = concat(
			transferable.controlPeriod().stream().map(income -> classify(income, false)),
			transferable.comparisonPeriodFallback().stream().map(income -> classify(income, true)))
			.toList();

		return new IncomeRulesResult(classified, detectChanges(present, periods), classifyAnswers(answers));
	}

	/**
	 * The per-organisation quality verdict from {@code Decision_ssbtekSvarKvalitet}.
	 * <p>
	 * When the table is not deployed, or returns nothing for a status, the answer degrades to
	 * {@code EJ_KONTROLLERBAR} rather than being assumed good. A missing regelverk must not read as "everything
	 * answered" - that is precisely the silent false negative this classification exists to prevent.
	 */
	private List<ClassifiedAgencyAnswer> classifyAnswers(final List<AgencyAnswer> answers) {
		return ofNullable(answers).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.map(this::classifyAnswer)
			.toList();
	}

	private ClassifiedAgencyAnswer classifyAnswer(final AgencyAnswer answer) {
		final Map<String, Object> row;
		try {
			row = evaluateFirst(ANSWER_QUALITY_DECISION_KEY, Map.of(
				"status", nullToEmpty(answer.status()),
				"ansokningsuppgiftFinns", answer.applicationInfoPresent(),
				"utbetalningarFinns", answer.paymentsPresent()));
		} catch (final RuntimeException e) {
			// The table is not published yet, or the engine refused it. Degrade rather than fail the whole
			// evaluation: an unavailable regelverk means we cannot vouch for the answer, not that it was fine.
			LOG.warn("Could not evaluate {} for {} ({}) - treating the answer as unverifiable: {}",
				ANSWER_QUALITY_DECISION_KEY, answer.organisation(), answer.agency(), e.getMessage());
			return new ClassifiedAgencyAnswer(answer, ClassifiedAgencyAnswer.QUALITY_UNVERIFIABLE,
				"Regelverket för svarskvalitet kunde inte utvärderas");
		}
		final var quality = ofNullable(str(row.get("kvalitet")))
			.filter(text -> !text.isBlank())
			.orElse(ClassifiedAgencyAnswer.QUALITY_UNVERIFIABLE);
		final var rule = ofNullable(str(row.get("regel")))
			.filter(text -> !text.isBlank())
			.orElse(null);
		return new ClassifiedAgencyAnswer(answer, quality, rule);
	}

	/** The two transferable groups, kept apart so caremanagement can filter the fallbacks against the previous month. */
	private record Transferable(List<SsbtekIncome> controlPeriod, List<SsbtekIncome> comparisonPeriodFallback) {}

	/** Control period incomes plus comparison period incomes whose benefit has no control period income. */
	private static Transferable selectTransferable(final List<SsbtekIncome> present, final SsbtekPeriods periods) {
		final var controlPeriodIncomes = present.stream().filter(income -> periods.isInControlPeriod(income.attributionDate())).toList();
		final var controlPeriodBenefits = benefitsIn(controlPeriodIncomes);
		final var comparisonPeriodFallbackIncomes = present.stream()
			.filter(income -> periods.isInComparisonPeriod(income.attributionDate()))
			.filter(income -> !controlPeriodBenefits.contains(normalize(income.benefit())))
			.toList();
		return new Transferable(controlPeriodIncomes, comparisonPeriodFallbackIncomes);
	}

	/**
	 * The benefits a period carries, by <em>presence</em> rather than by sum: {@link #sumByBenefit} drops an income
	 * whose net amount is absent, and a benefit SSBTEK reported without an amount is still a benefit that was reported.
	 */
	private static Set<String> benefitsIn(final List<SsbtekIncome> incomes) {
		return incomes.stream().map(income -> normalize(income.benefit())).collect(toSet());
	}

	/** The per-income allow-list verdict from {@code Decision_inkomstRalista}. */
	private ClassifiedIncome classify(final SsbtekIncome income, final boolean fromComparisonPeriod) {
		final var row = evaluateFirst(INCOME_ALLOW_LIST_DECISION_KEY, Map.of(
			"forman", nullToEmpty(income.benefit()),
			"delforman", nullToEmpty(income.subBenefit()),
			"beloppstyp", nullToEmpty(income.amountType())));
		return new ClassifiedIncome(income, str(row.get("atgard")), str(row.get("normberakning")),
			Boolean.TRUE.equals(row.get("varning")), str(row.get("regel")), fromComparisonPeriod);
	}

	/**
	 * Per-benefit change warnings: the comparison period net sum against the control period net sum, flagged according to
	 * the threshold {@code Decision_inkomstTroskel} gives for the benefit. A benefit that is new this month is compared
	 * with 0 on the other side, per verksamhetens "finns inte summa, sätt till 0" (Regelverk Drakel 2026-09-17).
	 *
	 * <p>
	 * A benefit that has <em>vanished</em> - present in the comparison period, absent from the control period - is
	 * deliberately left out. Verksamhetens revised regelverk treats föregående månad as facit and wants it said plainly:
	 * "INKOMST fanns föregående månad i SSBTEK men saknas nu", raised by caremanagement off the
	 * {@code jamforelseperiod} flag. Comparing it here as well would give the handläggare two rows for one fact, and
	 * the change would read as a measured -100 % rather than as an answer we never got.
	 * </p>
	 */
	private List<ChangeWarning> detectChanges(final List<SsbtekIncome> present, final SsbtekPeriods periods) {
		final var controlPeriodIncomes = present.stream().filter(income -> periods.isInControlPeriod(income.attributionDate())).toList();
		final var comparisonPeriodIncomes = present.stream().filter(income -> periods.isInComparisonPeriod(income.attributionDate())).toList();
		final var controlPeriodBenefits = benefitsIn(controlPeriodIncomes);
		final var controlSums = sumByBenefit(controlPeriodIncomes);
		final var comparisonSums = sumByBenefit(comparisonPeriodIncomes);
		final var displayNames = displayNames(concat(comparisonPeriodIncomes.stream(), controlPeriodIncomes.stream()).toList());

		return concat(comparisonSums.keySet().stream(), controlSums.keySet().stream()).distinct().sorted()
			.filter(controlPeriodBenefits::contains)
			.flatMap(benefit -> warningFor(displayNames.get(benefit),
				comparisonSums.getOrDefault(benefit, BigDecimal.ZERO),
				controlSums.getOrDefault(benefit, BigDecimal.ZERO)).stream())
			.toList();
	}

	/** The warning for one benefit, when the threshold from the DMN says the change is worth flagging. */
	private Optional<ChangeWarning> warningFor(final String benefit, final BigDecimal comparisonSum, final BigDecimal controlSum) {
		final var threshold = thresholdFor(benefit);
		if (threshold.isExact()) {
			return exactWarning(benefit, comparisonSum, controlSum, threshold);
		}
		return percentWarning(benefit, comparisonSum, controlSum, threshold);
	}

	/**
	 * Threshold 0 is verksamhetens exact comparison - "om samma summa = ingen varning, om olika summa = generera
	 * varning". It has to compare the sums themselves rather than the rounded percent: a 1250 -> 1255 kr change is 0,4 %,
	 * rounds to 0 %, and would slip past a percent comparison.
	 */
	private static Optional<ChangeWarning> exactWarning(final String benefit, final BigDecimal comparisonSum,
		final BigDecimal controlSum, final Threshold threshold) {
		if (controlSum.compareTo(comparisonSum) == 0) {
			return Optional.empty();
		}
		return Optional.of(new ChangeWarning(benefit, changePercentOrNull(comparisonSum, controlSum), comparisonSum, controlSum, threshold.rule()));
	}

	/** The percent comparison for the benefits verksamheten still allows a tolerance for. */
	private static Optional<ChangeWarning> percentWarning(final String benefit, final BigDecimal comparisonSum,
		final BigDecimal controlSum, final Threshold threshold) {
		if (comparisonSum.signum() == 0) {
			// a percentage of nothing says nothing; only the exact comparison can judge these
			return Optional.empty();
		}
		final var changePercent = changePercent(comparisonSum, controlSum);
		if (changePercent.abs().compareTo(threshold.percent()) <= 0) {
			return Optional.empty();
		}
		return Optional.of(new ChangeWarning(benefit, changePercent, comparisonSum, controlSum, threshold.rule()));
	}

	/** The change in percent, or {@code null} when there is no comparison sum to express it as a share of. */
	private static BigDecimal changePercentOrNull(final BigDecimal comparisonSum, final BigDecimal controlSum) {
		if (comparisonSum.signum() == 0) {
			return null;
		}
		return changePercent(comparisonSum, controlSum);
	}

	private static BigDecimal changePercent(final BigDecimal comparisonSum, final BigDecimal controlSum) {
		return controlSum.subtract(comparisonSum).multiply(HUNDRED).divide(comparisonSum.abs(), 0, RoundingMode.HALF_UP);
	}

	/** The threshold percent plus verksamhetens warning text from {@code Decision_inkomstTroskel}. */
	private record Threshold(BigDecimal percent, String rule) {

		/** Threshold 0 means "the sums must be identical", not "the rounded percent must be 0". */
		boolean isExact() {
			return percent.signum() == 0;
		}
	}

	private Threshold thresholdFor(final String benefit) {
		final var row = evaluateFirst(INCOME_THRESHOLD_DECISION_KEY, Map.of("forman", nullToEmpty(benefit)));
		final var percent = ofNullable(row.get("troskelProcent"))
			.map(value -> new BigDecimal(value.toString()))
			.orElse(DEFAULT_THRESHOLD_PERCENT);
		// regel is absent in tables published before 2026-09-17 - the warning then carries no verksamhetstext
		return new Threshold(percent, str(row.get("regel")));
	}

	private Map<String, Object> evaluateFirst(final String decisionKey, final Map<String, Object> variables) {
		final var rows = decisionService.evaluateDecisionByKey(decisionKey).variables(variables).evaluate().getResultList();
		if (rows.isEmpty()) {
			return Map.of();
		}
		return rows.getFirst();
	}

	private static Map<String, BigDecimal> sumByBenefit(final List<SsbtekIncome> incomes) {
		return incomes.stream()
			.filter(income -> income.netAmount() != null)
			.collect(groupingBy(income -> normalize(income.benefit()),
				mapping(SsbtekIncome::netAmount, reducing(BigDecimal.ZERO, BigDecimal::add))));
	}

	/** The benefit name as SSBTEK spelled it, keyed by its normalized form, for the warnings the case worker reads. */
	private static Map<String, String> displayNames(final List<SsbtekIncome> incomes) {
		return incomes.stream().collect(toMap(income -> normalize(income.benefit()),
			income -> nullToEmpty(income.benefit()), (first, second) -> first));
	}

	private static String normalize(final String value) {
		if (value == null) {
			return "";
		}
		return value.trim().toLowerCase();
	}

	private static String nullToEmpty(final String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	private static String str(final Object value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}
