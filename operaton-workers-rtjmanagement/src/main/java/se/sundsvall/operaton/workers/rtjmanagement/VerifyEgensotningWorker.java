package se.sundsvall.operaton.workers.rtjmanagement;

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
 * Runs the automated egensotning checks by delegating to rtj-management's verify endpoint and copies the resulting
 * routing {@code outcome} (+ informational detail flags) into process variables for the BPMN gateway. The actual
 * checks (bilaga present, folkbokförd at property, återansökan status) live in rtj-management, which owns the data.
 */
@Component
@TopicWorker(
	topic = "rtj-verify-egensotning",
	description = "Runs the automated egensotning checks (bilaga present, folkbokförd at property, återansökan status) via rtj-management and writes the routing outcome to process variables.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		VerifyEgensotningWorker.VAR_NAMESPACE,
		VerifyEgensotningWorker.VAR_ERRAND_ID
	},
	outputVariables = {
		VerifyEgensotningWorker.VAR_OUTCOME,
		VerifyEgensotningWorker.VAR_BILAGA_PRESENT,
		VerifyEgensotningWorker.VAR_REGISTERED_AT_PROPERTY,
		VerifyEgensotningWorker.VAR_REAPPLICATION_OK,
		VerifyEgensotningWorker.VAR_MANUAL_REVIEW_REASON,
		VerifyEgensotningWorker.VAR_DECISION_DESCRIPTION
	})
public class VerifyEgensotningWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_OUTCOME = "outcome";
	static final String VAR_BILAGA_PRESENT = "bilagaPresent";
	static final String VAR_REGISTERED_AT_PROPERTY = "registeredAtProperty";
	static final String VAR_REAPPLICATION_OK = "reapplicationOk";
	static final String VAR_MANUAL_REVIEW_REASON = "manualReviewReason";
	static final String VAR_DECISION_DESCRIPTION = "decisionDescription";

	private static final Logger LOG = LoggerFactory.getLogger(VerifyEgensotningWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public VerifyEgensotningWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-verify-egensotning.cron:*/5 * * * * *}", name = "rtj-verify-egensotning-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		final var result = rtjManagementClient.verifyEgensotning(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId);

		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_OUTCOME, result.getOutcome());
		output.put(VAR_BILAGA_PRESENT, result.getBilagaPresent());
		output.put(VAR_REGISTERED_AT_PROPERTY, result.getRegisteredAtProperty());
		output.put(VAR_REAPPLICATION_OK, result.getReapplicationOk());
		ofNullable(result.getManualReviewReason()).ifPresent(reason -> output.put(VAR_MANUAL_REVIEW_REASON, reason));
		ofNullable(result.getDecisionDescription()).ifPresent(description -> output.put(VAR_DECISION_DESCRIPTION, description));

		LOG.info("Verified egensotning errand {}: outcome={}", sanitizeForLogging(errandId), sanitizeForLogging(result.getOutcome()));
		return output;
	}
}
