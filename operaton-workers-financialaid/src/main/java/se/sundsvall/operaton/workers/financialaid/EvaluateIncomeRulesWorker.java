package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedIncome;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncome;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncomeExtractor;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

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
		EvaluateIncomeRulesWorker.VAR_OUT_HAS_WARNINGS
	})
public class EvaluateIncomeRulesWorker extends AbstractTopicWorker {

	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_FINANCIAL_AID_BASIS = "financialAidBasis";
	static final String VAR_CO_APPLICANT_BASIS = "coApplicantFinancialAidBasis";

	static final String VAR_OUT_CLASSIFIED = "classifiedIncomes";
	static final String VAR_OUT_UNHANDLED = "incomeUnhandled";
	static final String VAR_OUT_CHANGE_WARNINGS = "incomeChangeWarnings";
	static final String VAR_OUT_HAS_WARNINGS = "incomeHasWarnings";

	private static final String OFF_LIST_ACTION = "EJ_PA_LISTAN";

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

		final var incomes = new ArrayList<SsbtekIncome>(SsbtekIncomeExtractor.extract(parseBasis(requireVariable(task, VAR_FINANCIAL_AID_BASIS, String.class)), APPLICANT));
		optionalVariable(task, VAR_CO_APPLICANT_BASIS, String.class)
			.filter(json -> !json.isBlank())
			.ifPresent(json -> incomes.addAll(SsbtekIncomeExtractor.extract(parseBasis(json), CO_APPLICANT)));

		final var result = evaluator.evaluate(incomes, applicationMonth);

		final var unhandled = result.classified().stream()
			.filter(classified -> classified.warning() || OFF_LIST_ACTION.equals(classified.action()))
			.map(classified -> classified.income().benefit() + " (" + classified.action() + ")")
			.distinct()
			.toList();
		final var changeWarnings = result.changeWarnings().stream()
			.map(warning -> warning.benefit() + ": " + warning.changePercent() + "%")
			.toList();
		final var hasWarnings = !unhandled.isEmpty() || !changeWarnings.isEmpty();

		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_OUT_CLASSIFIED, serialize(result.classified()));
		output.put(VAR_OUT_UNHANDLED, String.join("; ", unhandled));
		output.put(VAR_OUT_CHANGE_WARNINGS, String.join("; ", changeWarnings));
		output.put(VAR_OUT_HAS_WARNINGS, hasWarnings);

		LOG.info("Income rules evaluated ({} transferable incomes, warnings: {})", result.classified().size(), hasWarnings);
		return output;
	}

	private Map<String, Object> parseBasis(final String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
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
