package se.sundsvall.operaton.workers.financialaid.regelverk;

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
 * Evaluates the SSBTEK regelverk over a household's incomes, in the process — the rules layer that used to live in
 * caremanagement. Applies the period transfer rule (kontroll + non-duplicated jämförelse), then evaluates the
 * runtime-published DMNs in-engine: {@code Decision_inkomstRalista} per transferable income (rålista verdict) and
 * {@code Decision_inkomstTroskel} per förmån (change-warning threshold). The decision tables are editable in the
 * modeler
 * without a code change, so the regelverk lives entirely in the engine.
 */
@Component
public class IncomeRegelverkEvaluator {

	static final String RALISTA_DECISION_KEY = "Decision_inkomstRalista";
	static final String TROSKEL_DECISION_KEY = "Decision_inkomstTroskel";

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final BigDecimal DEFAULT_THRESHOLD_PERCENT = BigDecimal.valueOf(12);

	private final DecisionService decisionService;

	public IncomeRegelverkEvaluator(final DecisionService decisionService) {
		this.decisionService = decisionService;
	}

	/**
	 * Evaluate the regelverk over the household's incomes for the application month.
	 *
	 * @param  incomes          the normalised SSBTEK incomes; may be {@code null}
	 * @param  applicationMonth the month the application concerns
	 * @return                  the transferable incomes with their per-income verdict, plus förmån-level change warnings
	 */
	public IncomeRegelverkResult evaluate(final List<SsbtekIncome> incomes, final YearMonth applicationMonth) {
		final var present = ofNullable(incomes).orElseGet(List::of).stream().filter(Objects::nonNull).toList();
		final var periods = SsbtekPeriods.forApplicationMonth(applicationMonth);

		final var classified = selectTransferable(present, periods).stream()
			.map(this::classify)
			.toList();

		return new IncomeRegelverkResult(classified, detectChanges(present, periods));
	}

	/** Kontrollperiod incomes plus jämförelseperiod incomes whose förmån has no kontrollperiod income. */
	private static List<SsbtekIncome> selectTransferable(final List<SsbtekIncome> present, final SsbtekPeriods periods) {
		final var kontroll = present.stream().filter(income -> periods.isInKontrollperiod(income.period())).toList();
		final Set<String> kontrollFormaner = kontroll.stream().map(income -> normalize(income.forman())).collect(toSet());
		final var jamforelseExtra = present.stream()
			.filter(income -> periods.isInJamforelseperiod(income.period()))
			.filter(income -> !kontrollFormaner.contains(normalize(income.forman())))
			.toList();
		return concat(kontroll.stream(), jamforelseExtra.stream()).toList();
	}

	/** The per-income rålista verdict from {@code Decision_inkomstRalista}. */
	private ClassifiedIncome classify(final SsbtekIncome income) {
		final var row = evaluateFirst(RALISTA_DECISION_KEY, Map.of(
			"forman", nullToEmpty(income.forman()),
			"delforman", nullToEmpty(income.delforman()),
			"beloppstyp", nullToEmpty(income.beloppstyp())));
		return new ClassifiedIncome(income, str(row.get("atgard")), str(row.get("normberakning")),
			Boolean.TRUE.equals(row.get("varning")), str(row.get("regel")));
	}

	/** Per-förmån change warnings: jämförelse vs kontroll net sum, flagged when the change exceeds the DMN threshold. */
	private List<ChangeWarning> detectChanges(final List<SsbtekIncome> present, final SsbtekPeriods periods) {
		final var kontrollSums = sumByForman(present.stream().filter(income -> periods.isInKontrollperiod(income.period())).toList());
		final var jamforelse = present.stream().filter(income -> periods.isInJamforelseperiod(income.period())).toList();
		final var displayNames = jamforelse.stream().collect(toMap(income -> normalize(income.forman()), SsbtekIncome::forman, (first, second) -> first));

		return sumByForman(jamforelse).entrySet().stream()
			.filter(entry -> entry.getValue().signum() != 0)
			.map(entry -> {
				final var jamforelseSum = entry.getValue();
				final var kontrollSum = kontrollSums.getOrDefault(entry.getKey(), BigDecimal.ZERO);
				final var changePercent = kontrollSum.subtract(jamforelseSum).multiply(HUNDRED)
					.divide(jamforelseSum.abs(), 0, RoundingMode.HALF_UP);
				return new ChangeWarning(displayNames.get(entry.getKey()), changePercent, jamforelseSum, kontrollSum);
			})
			.filter(warning -> warning.changePercent().abs().compareTo(thresholdFor(warning.forman())) > 0)
			.toList();
	}

	private BigDecimal thresholdFor(final String forman) {
		return ofNullable(evaluateFirst(TROSKEL_DECISION_KEY, Map.of("forman", nullToEmpty(forman))).get("troskelProcent"))
			.map(value -> new BigDecimal(value.toString()))
			.orElse(DEFAULT_THRESHOLD_PERCENT);
	}

	private Map<String, Object> evaluateFirst(final String decisionKey, final Map<String, Object> variables) {
		final var rows = decisionService.evaluateDecisionByKey(decisionKey).variables(variables).evaluate().getResultList();
		return rows.isEmpty() ? Map.of() : rows.getFirst();
	}

	private static Map<String, BigDecimal> sumByForman(final List<SsbtekIncome> incomes) {
		return incomes.stream()
			.filter(income -> income.netAmount() != null)
			.collect(groupingBy(income -> normalize(income.forman()),
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
