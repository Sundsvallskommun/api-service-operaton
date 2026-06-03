package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Permit;
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
 * Issues an LBE permit (tillstånd) on an errand in RtjManagement when the tillståndsbeslut is taken.
 * The validity period is computed server-side by rtj-management from {@code permitType} (5 år
 * brandfarlig / högst 3 år explosiv, förlängt till nästa fasta datum), so this worker only needs to
 * pass the type (and optionally conditions/villkor).
 */
@Component
@TopicWorker(
	topic = "rtj-create-permit",
	description = "Issues an LBE permit (tillstånd) on an errand in RtjManagement. Validity period is computed server-side from permitType (5 år brandfarlig / högst 3 år explosiv → fasta datum).",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreatePermitWorker.VAR_NAMESPACE,
		CreatePermitWorker.VAR_ERRAND_ID,
		CreatePermitWorker.VAR_PERMIT_TYPE,
		CreatePermitWorker.VAR_CONDITIONS
	})
public class CreatePermitWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_PERMIT_TYPE = "permitType";
	static final String VAR_CONDITIONS = "conditions";

	private static final Logger LOG = LoggerFactory.getLogger(CreatePermitWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public CreatePermitWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-create-permit.cron:*/5 * * * * *}", name = "rtj-create-permit-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var permit = new Permit().permitType(requireVariable(task, VAR_PERMIT_TYPE, String.class));
		optionalVariable(task, VAR_CONDITIONS, String.class).ifPresent(permit::conditions);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		rtjManagementClient.createPermit(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			permit);

		LOG.info("Permit (type {}) issued on errand {}", sanitizeForLogging(permit.getPermitType()), sanitizeForLogging(errandId));
		return emptyOutput();
	}
}
