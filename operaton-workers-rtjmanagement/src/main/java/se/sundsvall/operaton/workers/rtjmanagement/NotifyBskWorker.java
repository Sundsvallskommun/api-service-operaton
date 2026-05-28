package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Stakeholder;
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
	topic = "rtj-notify-bsk",
	description = "Assigns a BSK (Brandskyddskontrollant) to the errand by adding a stakeholder with role=BSK. Replaces the legacy 'Meddela BSK via Teams' step in the Daedalos flow.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		NotifyBskWorker.VAR_NAMESPACE,
		NotifyBskWorker.VAR_ERRAND_ID
	})
public class NotifyBskWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";

	private static final String BSK_ROLE = "BSK";
	private static final String BSK_FIRST_NAME = "BSK";
	private static final String BSK_LAST_NAME = "Handläggare";

	private static final Logger LOG = LoggerFactory.getLogger(NotifyBskWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public NotifyBskWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-notify-bsk.cron:*/5 * * * * *}", name = "rtj-notify-bsk-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var stakeholder = new Stakeholder()
			.role(BSK_ROLE)
			.firstName(BSK_FIRST_NAME)
			.lastName(BSK_LAST_NAME);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		rtjManagementClient.createErrandStakeholder(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			stakeholder);

		LOG.info("BSK assigned to errand {}", sanitizeForLogging(errandId));
		return emptyOutput();
	}
}
