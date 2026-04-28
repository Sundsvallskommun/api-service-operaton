package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Parameter;
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

@Component
@TopicWorker(
	topic = "update-errand-parameter",
	description = "Adds a parameter (key + values) to an errand in CareManagement. Used by processes that compute a result and need to surface it on the originating errand for handläggare review.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		UpdateErrandParameterWorker.VAR_NAMESPACE,
		UpdateErrandParameterWorker.VAR_ERRAND_ID,
		UpdateErrandParameterWorker.VAR_PARAMETER_KEY,
		UpdateErrandParameterWorker.VAR_PARAMETER_VALUE,
		UpdateErrandParameterWorker.VAR_PARAMETER_DISPLAY_NAME,
		UpdateErrandParameterWorker.VAR_PARAMETER_GROUP
	})
public class UpdateErrandParameterWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_PARAMETER_KEY = "parameterKey";
	static final String VAR_PARAMETER_VALUE = "parameterValue";
	static final String VAR_PARAMETER_DISPLAY_NAME = "parameterDisplayName";
	static final String VAR_PARAMETER_GROUP = "parameterGroup";

	private static final Logger LOG = LoggerFactory.getLogger(UpdateErrandParameterWorker.class);

	private final CareManagementClient careManagementClient;

	public UpdateErrandParameterWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.update-errand-parameter.cron:*/5 * * * * *}", name = "update-errand-parameter-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var parameter = new Parameter()
			.key(requireVariable(task, VAR_PARAMETER_KEY, String.class))
			.values(List.of(requireVariable(task, VAR_PARAMETER_VALUE, String.class)));
		optionalVariable(task, VAR_PARAMETER_DISPLAY_NAME, String.class).ifPresent(parameter::displayName);
		optionalVariable(task, VAR_PARAMETER_GROUP, String.class).ifPresent(parameter::parameterGroup);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		careManagementClient.createErrandParameter(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			parameter);

		LOG.info("Errand parameter {} added to errand {}", parameter.getKey(), errandId);
		return emptyOutput();
	}
}
