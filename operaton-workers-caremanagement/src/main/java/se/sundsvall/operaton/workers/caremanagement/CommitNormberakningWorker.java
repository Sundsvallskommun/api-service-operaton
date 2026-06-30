package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
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

import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Creates the EB normberäkning in Lifecare via CareManagement from the incomes classified by the operaton regelverk —
 * called once a beslut is taken, never during the daily SSBTEK loop (that is {@code prepare-normberakning}). Outputs
 * the
 * created Lifecare calculation id.
 */
@Component
@TopicWorker(
	topic = "commit-normberakning",
	description = "Creates the EB normberäkning in Lifecare via CareManagement from the classified incomes — called once a beslut is taken, never during the daily SSBTEK loop. CareManagement resolves each income's category to an FC type id, assembles and posts the normberäkning. Outputs the created Lifecare calculation id.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CommitNormberakningWorker.VAR_NAMESPACE,
		CommitNormberakningWorker.VAR_APPLICANT,
		CommitNormberakningWorker.VAR_CO_APPLICANT,
		CommitNormberakningWorker.VAR_APPLICATION_MONTH,
		CommitNormberakningWorker.VAR_ERRAND_ID,
		CommitNormberakningWorker.VAR_CLASSIFIED_INCOMES,
		CommitNormberakningWorker.VAR_UNHANDLED_INCOMES,
		CommitNormberakningWorker.VAR_CHANGE_WARNINGS
	},
	outputVariables = {
		CommitNormberakningWorker.VAR_OUT_CALCULATION_ID
	})
public class CommitNormberakningWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_CLASSIFIED_INCOMES = "classifiedIncomes";
	static final String VAR_UNHANDLED_INCOMES = "unhandledIncomes";
	static final String VAR_CHANGE_WARNINGS = "changeWarnings";

	static final String VAR_OUT_CALCULATION_ID = "normberakningCalculationId";

	private static final Logger LOG = LoggerFactory.getLogger(CommitNormberakningWorker.class);

	private final CareManagementClient careManagementClient;

	public CommitNormberakningWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.commit-normberakning.cron:*/5 * * * * *}", name = "commit-normberakning-worker", lockAtMostFor = "PT30S")
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
		optionalVariable(task, VAR_UNHANDLED_INCOMES, String.class).map(CommitNormberakningWorker::split).ifPresent(request::unhandledIncomes);
		optionalVariable(task, VAR_CHANGE_WARNINGS, String.class).map(CommitNormberakningWorker::split).ifPresent(request::changeWarnings);

		final var response = careManagementClient.commitNormberakning(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		final var calculationId = ofNullable(response).map(NormberakningResponse::getCalculationId).orElse(null);

		LOG.info("Normberäkning {} created in Lifecare via CareManagement", sanitizeForLogging(String.valueOf(calculationId)));
		return ofNullable(calculationId)
			.map(id -> Map.<String, Object>of(VAR_OUT_CALCULATION_ID, id))
			.orElseGet(Map::of);
	}

	/** Split a "; "-joined warning string (as produced by evaluate-income-regelverk) back into a list. */
	private static List<String> split(final String joined) {
		return joined.isBlank() ? List.of() : List.of(joined.split("; "));
	}
}
