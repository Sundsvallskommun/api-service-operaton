package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.ActualisationRequest;
import generated.se.sundsvall.caremanagement.ActualisationResponse;
import java.util.HashMap;
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
 * Creates the Lifecare aktualisering (case intake) for an ekonomiskt-bistånd errand — the first process step that lands
 * the case in Lifecare. Delegates to CareManagement's {@code financial-assistance/actualisation} endpoint, which builds
 * the aktualisering against the applicant's Lifecare FC proposal, creates it, and (when an errandId is present) records
 * a
 * {@code Decision(ACTUALISATION)} on the errand. Outputs the created aktualisering id.
 */
@Component
@TopicWorker(
	topic = "create-actualisation",
	description = "Creates the Lifecare aktualisering (case intake) for an ekonomiskt-bistånd errand via CareManagement and records a Decision(ACTUALISATION) on the errand. Outputs the created aktualisering id. This is the process's first Lifecare write; the normberäkning step later links its calculation to this aktualisering.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateActualisationWorker.VAR_NAMESPACE,
		CreateActualisationWorker.VAR_APPLICANT,
		CreateActualisationWorker.VAR_APPLICATION_MONTH,
		CreateActualisationWorker.VAR_ERRAND_ID
	},
	outputVariables = {
		CreateActualisationWorker.VAR_OUT_ACTUALISATION_ID
	})
public class CreateActualisationWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_ERRAND_ID = "errandId";

	static final String VAR_OUT_ACTUALISATION_ID = "actualisationId";

	private static final Logger LOG = LoggerFactory.getLogger(CreateActualisationWorker.class);

	private final CareManagementClient careManagementClient;

	public CreateActualisationWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.create-actualisation.cron:*/5 * * * * *}", name = "create-actualisation-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var request = new ActualisationRequest()
			.applicant(requireVariable(task, VAR_APPLICANT, String.class))
			.applicationMonth(requireVariable(task, VAR_APPLICATION_MONTH, String.class));
		optionalVariable(task, VAR_ERRAND_ID, String.class).ifPresent(request::errandId);

		final var response = careManagementClient.createActualisation(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		final var actualisationId = ofNullable(response).map(ActualisationResponse::getActualisationId).orElse(null);

		final Map<String, Object> output = new HashMap<>();
		ofNullable(actualisationId).ifPresent(id -> output.put(VAR_OUT_ACTUALISATION_ID, id));

		LOG.info("Aktualisering {} created via CareManagement", sanitizeForLogging(String.valueOf(actualisationId)));
		return output;
	}
}
