package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

/**
 * Prepares the EB normberäkning each daily loop <strong>without</strong> writing to Lifecare. CareManagement reports
 * whether this month's classified incomes cover every income type the previous normberäkning had ({@code
 * informationComplete} + {@code missingIncomeTypes}), records the income warnings as a single Decision(RECOMMENDATION),
 * and reflects completeness in the errand status (KOMPLETTERING ⇄ VANTAR_PA_BESLUT). The Lifecare normberäkning itself
 * is created only after a beslut — see {@code commit-normberakning}.
 */
@Component
@TopicWorker(
	topic = "prepare-normberakning",
	description = "Prepares the EB normberäkning each daily loop without writing to Lifecare: CareManagement reports completeness (does this month cover every income type the previous normberäkning had?), records the income warnings as a Decision(RECOMMENDATION), and sets the errand status (KOMPLETTERING while incomplete, VANTAR_PA_BESLUT when complete). Outputs informationComplete + missingIncomeTypes. The Lifecare normberäkning itself is created only after a beslut (commit-normberakning).",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		PrepareNormberakningWorker.VAR_NAMESPACE,
		PrepareNormberakningWorker.VAR_APPLICANT,
		PrepareNormberakningWorker.VAR_CO_APPLICANT,
		PrepareNormberakningWorker.VAR_APPLICATION_MONTH,
		PrepareNormberakningWorker.VAR_ERRAND_ID,
		PrepareNormberakningWorker.VAR_CLASSIFIED_INCOMES,
		PrepareNormberakningWorker.VAR_UNHANDLED_INCOMES,
		PrepareNormberakningWorker.VAR_CHANGE_WARNINGS
	},
	outputVariables = {
		PrepareNormberakningWorker.VAR_OUT_INFORMATION_COMPLETE,
		PrepareNormberakningWorker.VAR_OUT_MISSING_INCOME_TYPES,
		PrepareNormberakningWorker.VAR_OUT_HAS_WARNINGS
	})
public class PrepareNormberakningWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_CLASSIFIED_INCOMES = "classifiedIncomes";
	static final String VAR_UNHANDLED_INCOMES = "unhandledIncomes";
	static final String VAR_CHANGE_WARNINGS = "changeWarnings";

	// Completeness of this month's normberäkning vs the previous month's — drives the process's daily SSBTEK poll loop.
	static final String VAR_OUT_INFORMATION_COMPLETE = "informationComplete";
	static final String VAR_OUT_MISSING_INCOME_TYPES = "missingIncomeTypes";
	static final String VAR_OUT_HAS_WARNINGS = "normberakningHasWarnings";

	private static final Logger LOG = LoggerFactory.getLogger(PrepareNormberakningWorker.class);

	private final CareManagementClient careManagementClient;

	public PrepareNormberakningWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.prepare-normberakning.cron:*/5 * * * * *}", name = "prepare-normberakning-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var request = new NormberakningRequest()
			.applicant(requireVariable(task, VAR_APPLICANT, String.class))
			.applicationMonth(requireVariable(task, VAR_APPLICATION_MONTH, String.class));
		optionalVariable(task, VAR_CO_APPLICANT, String.class).ifPresent(request::coApplicant);
		optionalVariable(task, VAR_ERRAND_ID, String.class).ifPresent(request::errandId);
		optionalVariable(task, VAR_CLASSIFIED_INCOMES, String.class).ifPresent(request::classifiedIncomes);
		optionalVariable(task, VAR_UNHANDLED_INCOMES, String.class).map(PrepareNormberakningWorker::split).ifPresent(request::unhandledIncomes);
		optionalVariable(task, VAR_CHANGE_WARNINGS, String.class).map(PrepareNormberakningWorker::split).ifPresent(request::changeWarnings);

		final var response = careManagementClient.prepareNormberakning(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		// Absent/unknown completeness ⇒ treat as complete so the process is never wedged polling forever.
		final var informationComplete = ofNullable(response).map(NormberakningResponse::getInformationComplete).orElse(TRUE);
		final var missingIncomeTypes = ofNullable(response).map(NormberakningResponse::getMissingIncomeTypes).orElseGet(List::of);
		final var unhandledIncomes = ofNullable(response).map(NormberakningResponse::getUnhandledIncomes).orElseGet(List::of);
		final var changeWarnings = ofNullable(response).map(NormberakningResponse::getChangeWarnings).orElseGet(List::of);
		final var hasWarnings = !unhandledIncomes.isEmpty() || !changeWarnings.isEmpty() || !missingIncomeTypes.isEmpty();

		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_OUT_INFORMATION_COMPLETE, informationComplete);
		output.put(VAR_OUT_MISSING_INCOME_TYPES, String.join("; ", missingIncomeTypes));
		output.put(VAR_OUT_HAS_WARNINGS, hasWarnings);

		LOG.info("Normberäkning prepared via CareManagement (information complete: {}, warnings: {})", informationComplete, hasWarnings);
		return output;
	}

	/** Split a "; "-joined warning string (as produced by evaluate-income-regelverk) back into a list. */
	private static List<String> split(final String joined) {
		return joined.isBlank() ? List.of() : List.of(joined.split("; "));
	}
}
