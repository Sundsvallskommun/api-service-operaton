package se.sundsvall.operaton.workers.rtjmanagement;

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

/**
 * Revokes (återkallar) all active LBE permits on an errand in RtjManagement — 20 § LBE. Triggered by
 * the återkalla event subprocess in the LBE processes.
 */
@Component
@TopicWorker(
	topic = "rtj-revoke-permit",
	description = "Revokes (återkallar) all active LBE permits on an errand in RtjManagement (20 § LBE).",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		RtjRevokePermitWorker.VAR_NAMESPACE,
		RtjRevokePermitWorker.VAR_ERRAND_ID
	})
public class RtjRevokePermitWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";

	private static final Logger LOG = LoggerFactory.getLogger(RtjRevokePermitWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public RtjRevokePermitWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-revoke-permit.cron:*/5 * * * * *}", name = "rtj-revoke-permit-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		rtjManagementClient.revokePermits(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId);

		LOG.info("Permits revoked on errand {}", sanitizeForLogging(errandId));
		return emptyOutput();
	}
}
