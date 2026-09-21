package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.financialaid.rules.AgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.ChangeWarning;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedAgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedIncome;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekAvailability;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncome;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncomeExtractor;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.util.stream.Stream.concat;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.CO_APPLICANT;

/**
 * Evaluates the SSBTEK income rules in the process. Parses the household's financial-aid basis (the
 * {@code financialAidBasis} JSON from {@code fetch-financial-aid-basis}, applicant plus optional co-applicant),
 * extracts the incomes, and runs them through {@link IncomeRulesEvaluator}. Outputs the classified incomes as JSON for
 * caremanagement to assemble and post to Lifecare, plus the unhandled-income and change warnings for the case worker.
 */
@Component
@TopicWorker(
	topic = "evaluate-income-regelverk",
	description = "Evaluates the SSBTEK income rules (allow list, thresholds, and period rules via the published DMNs) over the household's financial-aid income basis and outputs the classified incomes (JSON) for caremanagement plus the unhandled/change warnings. The rules live entirely in the engine; caremanagement no longer evaluates them.",
	inputVariables = {
		EvaluateIncomeRulesWorker.VAR_APPLICATION_MONTH,
		EvaluateIncomeRulesWorker.VAR_FINANCIAL_AID_BASIS,
		EvaluateIncomeRulesWorker.VAR_CO_APPLICANT_BASIS
	},
	outputVariables = {
		EvaluateIncomeRulesWorker.VAR_OUT_CLASSIFIED,
		EvaluateIncomeRulesWorker.VAR_OUT_UNHANDLED,
		EvaluateIncomeRulesWorker.VAR_OUT_CHANGE_WARNINGS,
		EvaluateIncomeRulesWorker.VAR_OUT_HAS_WARNINGS,
		EvaluateIncomeRulesWorker.VAR_OUT_SSBTEK_ERROR
	})
public class EvaluateIncomeRulesWorker extends AbstractTopicWorker {

	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_FINANCIAL_AID_BASIS = "financialAidBasis";
	static final String VAR_CO_APPLICANT_BASIS = "coApplicantFinancialAidBasis";

	static final String VAR_OUT_CLASSIFIED = "classifiedIncomes";
	static final String VAR_OUT_UNHANDLED = "incomeUnhandled";
	static final String VAR_OUT_CHANGE_WARNINGS = "incomeChangeWarnings";
	static final String VAR_OUT_HAS_WARNINGS = "incomeHasWarnings";
	static final String VAR_OUT_SSBTEK_ERROR = "ssbtekError";

	private static final String OFF_LIST_ACTION = "EJ_PA_LISTAN";
	private static final String EMPTY_BASIS = "{}";

	private static final Logger LOG = LoggerFactory.getLogger(EvaluateIncomeRulesWorker.class);

	private final IncomeRulesEvaluator evaluator;
	private final ObjectMapper objectMapper;

	public EvaluateIncomeRulesWorker(final ExternalTaskService externalTaskService, final IncomeRulesEvaluator evaluator, final ObjectMapper objectMapper) {
		super(externalTaskService);
		this.evaluator = evaluator;
		this.objectMapper = objectMapper;
	}

	@Dept44Scheduled(cron = "${scheduler.evaluate-income-regelverk.cron:*/5 * * * * *}", name = "evaluate-income-regelverk-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var applicationMonth = YearMonth.parse(requireVariable(task, VAR_APPLICATION_MONTH, String.class));

		final var applicantBasisJson = requireVariable(task, VAR_FINANCIAL_AID_BASIS, String.class);
		final var coApplicantBasisJson = optionalVariable(task, VAR_CO_APPLICANT_BASIS, String.class)
			.filter(json -> !json.isBlank())
			.orElse(EMPTY_BASIS);

		// Verksamhetens regelverk: when SSBTEK could not be read, the rules must not run at all — the handläggare gets
		// the read-failure warning instead and the daily loop tries again. Rules run over a basis we could not read
		// report an income we never saw as an income the sökande does not have.
		if (SsbtekAvailability.hasReadFailure(readTree(applicantBasisJson)) || SsbtekAvailability.hasReadFailure(readTree(coApplicantBasisJson))) {
			LOG.warn("SSBTEK could not be read — skipping the income rules for this run");
			return readFailureOutput();
		}

		final var applicantBasis = parseBasis(applicantBasisJson);
		final var coApplicantBasis = parseBasis(coApplicantBasisJson);
		final var incomes = new ArrayList<SsbtekIncome>(SsbtekIncomeExtractor.extract(applicantBasis, APPLICANT));
		final var answers = new ArrayList<AgencyAnswer>(SsbtekIncomeExtractor.extractAnswers(applicantBasis));
		if (!coApplicantBasis.isEmpty()) {
			incomes.addAll(SsbtekIncomeExtractor.extract(coApplicantBasis, CO_APPLICANT));
			answers.addAll(SsbtekIncomeExtractor.extractAnswers(coApplicantBasis));
		}

		final var result = evaluator.evaluate(incomes, answers, applicationMonth);

		// An organisation whose answer could not be verified is an unhandled item for the handläggare, not an absence
		// of income: SSBTEK answering "I cannot say" must never read as "this person has nothing".
		final var unverifiable = result.answers().stream()
			.filter(ClassifiedAgencyAnswer::unverifiable)
			.map(EvaluateIncomeRulesWorker::renderAnswer)
			.distinct()
			.toList();
		final var unhandled = concat(
			result.classified().stream()
				.filter(classified -> classified.warning() || OFF_LIST_ACTION.equals(classified.action()))
				.map(classified -> classified.income().benefit() + " (" + classified.action() + ")"),
			unverifiable.stream())
			.distinct()
			.toList();
		final var changeWarnings = result.changeWarnings().stream()
			.map(EvaluateIncomeRulesWorker::render)
			.toList();
		final var hasWarnings = !unhandled.isEmpty() || !changeWarnings.isEmpty();

		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_OUT_CLASSIFIED, serialize(result.classified()));
		output.put(VAR_OUT_UNHANDLED, String.join("; ", unhandled));
		output.put(VAR_OUT_CHANGE_WARNINGS, String.join("; ", changeWarnings));
		output.put(VAR_OUT_HAS_WARNINGS, hasWarnings);
		output.put(VAR_OUT_SSBTEK_ERROR, false);

		LOG.info("Income rules evaluated ({} transferable incomes, warnings: {})", result.classified().size(), hasWarnings);
		return output;
	}

	/**
	 * The output of a run where SSBTEK could not be read. The classified incomes are left <strong>blank</strong> rather
	 * than an empty JSON list: caremanagement reads an empty list as "this month has no incomes" and would clear the
	 * draft rows the previous run transferred. The blank string plus {@code ssbtekError} tells it to leave the
	 * calculation exactly as it stands and only raise the warning.
	 */
	private static Map<String, Object> readFailureOutput() {
		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_OUT_CLASSIFIED, "");
		output.put(VAR_OUT_UNHANDLED, "");
		output.put(VAR_OUT_CHANGE_WARNINGS, "");
		output.put(VAR_OUT_HAS_WARNINGS, true);
		output.put(VAR_OUT_SSBTEK_ERROR, true);
		return output;
	}

	/**
	 * The case worker's one-line rendering of a change warning: the change in percent, or the two sums when there is no
	 * comparison sum to take a percentage of, followed by verksamhetens warning text from the DMN when the deployed
	 * table carries one.
	 */
	private static String render(final ChangeWarning warning) {
		final var change = warning.benefit() + ": " + changeText(warning);
		if (hasText(warning.rule())) {
			return change + " – " + warning.rule();
		}
		return change;
	}

	/**
	 * A verified-nothing and an unanswerable organisation look identical in the income list, so the text has to say
	 * which one this is - phrased as a statement about our check, never about the sökande.
	 */
	private static String renderAnswer(final ClassifiedAgencyAnswer classified) {
		final var organisation = classified.answer().organisation();
		final var who = "A-kassa " + organisation;
		if (!hasText(organisation)) {
			return "A-kassa (okänd organisation): kunde inte kontrolleras";
		}
		if (hasText(classified.rule())) {
			return who + ": kunde inte kontrolleras – " + classified.rule();
		}
		return who + ": kunde inte kontrolleras";
	}

	private static String changeText(final ChangeWarning warning) {
		if (warning.changePercent() == null) {
			return warning.comparisonSum() + " kr → " + warning.controlSum() + " kr";
		}
		return warning.changePercent() + "%";
	}

	private Map<String, Object> parseBasis(final String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Failed to parse financial-aid basis JSON", e);
		}
	}

	/**
	 * The basis as a JSON tree, for the availability check. Deliberately not the {@code Map} above: the FEEL engine's
	 * Jackson module binds nested objects to its own map type, so a type-based test on the map's values misses an
	 * agency error entirely.
	 */
	private JsonNode readTree(final String json) {
		try {
			return objectMapper.readTree(json);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Failed to parse financial-aid basis JSON", e);
		}
	}

	private String serialize(final List<ClassifiedIncome> classified) {
		try {
			return objectMapper.writeValueAsString(classified);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize classified incomes", e);
		}
	}
}
