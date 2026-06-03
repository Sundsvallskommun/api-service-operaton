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
 * Runs the automated brandfarlig-vara completeness check by delegating to rtj-management's verify endpoint and copies
 * the resulting routing {@code outcome} (+ informational detail flags + generated decision text) into process variables
 * for the BPMN gateway. The actual check (hanteringsplats, minst en brandfarlig vara, minst en bilaga) lives in
 * rtj-management, which owns the data. A brandfarlig-vara permit is never auto-approved — the outcome is
 * {@code NEEDS_SUPPLEMENT} or {@code NEEDS_MANUAL_REVIEW}.
 */
@Component
@TopicWorker(
	topic = "rtj-verify-brandfarlig-vara",
	description = "Runs the automated brandfarlig-vara completeness check (hanteringsplats, brandfarliga varor, bilaga) via rtj-management and writes the routing outcome to process variables.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		VerifyBrandfarligVaraWorker.VAR_NAMESPACE,
		VerifyBrandfarligVaraWorker.VAR_ERRAND_ID
	},
	outputVariables = {
		VerifyBrandfarligVaraWorker.VAR_OUTCOME,
		VerifyBrandfarligVaraWorker.VAR_BILAGA_PRESENT,
		VerifyBrandfarligVaraWorker.VAR_PRODUCTS_PRESENT,
		VerifyBrandfarligVaraWorker.VAR_SUPPLEMENT_REASON,
		VerifyBrandfarligVaraWorker.VAR_DECISION_DESCRIPTION
	})
public class VerifyBrandfarligVaraWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_OUTCOME = "outcome";
	static final String VAR_BILAGA_PRESENT = "bilagaPresent";
	static final String VAR_PRODUCTS_PRESENT = "productsPresent";
	static final String VAR_SUPPLEMENT_REASON = "supplementReason";
	static final String VAR_DECISION_DESCRIPTION = "decisionDescription";

	private static final Logger LOG = LoggerFactory.getLogger(VerifyBrandfarligVaraWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public VerifyBrandfarligVaraWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-verify-brandfarlig-vara.cron:*/5 * * * * *}", name = "rtj-verify-brandfarlig-vara-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		final var result = rtjManagementClient.verifyBrandfarligVara(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId);

		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_OUTCOME, result.getOutcome());
		output.put(VAR_BILAGA_PRESENT, result.getBilagaPresent());
		output.put(VAR_PRODUCTS_PRESENT, result.getProductsPresent());
		ofNullable(result.getSupplementReason()).ifPresent(reason -> output.put(VAR_SUPPLEMENT_REASON, reason));
		ofNullable(result.getDecisionDescription()).ifPresent(description -> output.put(VAR_DECISION_DESCRIPTION, description));

		LOG.info("Verified brandfarlig-vara errand {}: outcome={}", sanitizeForLogging(errandId), sanitizeForLogging(result.getOutcome()));
		return output;
	}
}
