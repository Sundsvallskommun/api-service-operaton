package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Decision;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Component
@TopicWorker(
	topic = "rtj-add-errand-decision",
	description = "Records a decision (system-generated or human) on an errand in RtjManagement. Used by processes that produce a recommendation, evaluation, or other decision the handläggare needs to see on the errand for review.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		RtjAddErrandDecisionWorker.VAR_NAMESPACE,
		RtjAddErrandDecisionWorker.VAR_ERRAND_ID,
		RtjAddErrandDecisionWorker.VAR_DECISION_TYPE,
		RtjAddErrandDecisionWorker.VAR_VALUE,
		RtjAddErrandDecisionWorker.VAR_DESCRIPTION,
		RtjAddErrandDecisionWorker.VAR_CREATED_BY
	})
public class RtjAddErrandDecisionWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_DECISION_TYPE = "decisionType";
	static final String VAR_VALUE = "value";
	static final String VAR_DESCRIPTION = "description";
	static final String VAR_CREATED_BY = "createdBy";

	private static final Logger LOG = LoggerFactory.getLogger(RtjAddErrandDecisionWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public RtjAddErrandDecisionWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-add-errand-decision.cron:*/5 * * * * *}", name = "rtj-add-errand-decision-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var decision = new Decision()
			.decisionType(requireVariable(task, VAR_DECISION_TYPE, String.class))
			.value(requireVariable(task, VAR_VALUE, String.class));
		optionalVariable(task, VAR_DESCRIPTION, String.class).ifPresent(decision::description);
		optionalVariable(task, VAR_CREATED_BY, String.class).ifPresent(decision::createdBy);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		rtjManagementClient.createErrandDecision(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			decision);

		final var sanitizedValue = sanitizeForLogging(decision.getValue());
		final var sanitizedType = sanitizeForLogging(decision.getDecisionType());
		final var sanitizedErrandId = sanitizeForLogging(errandId);
		LOG.info("Decision {} (type {}) added to errand {}", sanitizedValue, sanitizedType, sanitizedErrandId);
		return emptyOutput();
	}
}
