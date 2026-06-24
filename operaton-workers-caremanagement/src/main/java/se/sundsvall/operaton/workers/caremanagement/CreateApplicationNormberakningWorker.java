package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
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
 * Creates the EB normberäkning in Lifecare via CareManagement straight from the data the citizen declared in a
 * nyansökan — no SSBTEK, no daily loop, no caseworker draft. CareManagement reads the application's own incomes, costs
 * and household off the errand, resolves them to FC types and posts the calculation in one shot. Outputs the created
 * Lifecare calculation id.
 */
@Component
@TopicWorker(
	topic = "create-application-normberakning",
	description = "Creates the EB normberäkning in Lifecare via CareManagement straight from the application (nyansökan) — no SSBTEK, no daily loop, no caseworker draft. CareManagement builds the calculation from the application's own incomes, costs and household and posts it. Outputs the created Lifecare calculation id.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateApplicationNormberakningWorker.VAR_NAMESPACE,
		CreateApplicationNormberakningWorker.VAR_APPLICANT,
		CreateApplicationNormberakningWorker.VAR_CO_APPLICANT,
		CreateApplicationNormberakningWorker.VAR_APPLICATION_MONTH,
		CreateApplicationNormberakningWorker.VAR_ERRAND_ID
	},
	outputVariables = {
		CreateApplicationNormberakningWorker.VAR_OUT_CALCULATION_ID
	})
public class CreateApplicationNormberakningWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_ERRAND_ID = "errandId";

	static final String VAR_OUT_CALCULATION_ID = "normberakningCalculationId";

	private static final Logger LOG = LoggerFactory.getLogger(CreateApplicationNormberakningWorker.class);

	private final CareManagementClient careManagementClient;

	public CreateApplicationNormberakningWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.create-application-normberakning.cron:*/5 * * * * *}", name = "create-application-normberakning-worker", lockAtMostFor = "PT30S")
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

		final var response = careManagementClient.createApplicationNormberakning(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		final var calculationId = ofNullable(response).map(NormberakningResponse::getCalculationId).orElse(null);

		LOG.info("Normberäkning {} created in Lifecare via CareManagement from the application", sanitizeForLogging(String.valueOf(calculationId)));
		return ofNullable(calculationId)
			.map(id -> Map.<String, Object>of(VAR_OUT_CALCULATION_ID, id))
			.orElseGet(Map::of);
	}
}
