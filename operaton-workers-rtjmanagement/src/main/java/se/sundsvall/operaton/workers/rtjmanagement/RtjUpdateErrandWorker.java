package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.PatchErrand;
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
	topic = "rtj-update-errand",
	description = "Updates an errand in RtjManagement",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		RtjUpdateErrandWorker.VAR_NAMESPACE,
		RtjUpdateErrandWorker.VAR_ERRAND_ID,
		RtjUpdateErrandWorker.VAR_TITLE,
		RtjUpdateErrandWorker.VAR_STATUS,
		RtjUpdateErrandWorker.VAR_PRIORITY,
		RtjUpdateErrandWorker.VAR_DESCRIPTION,
		RtjUpdateErrandWorker.VAR_REPORTER_USER_ID,
		RtjUpdateErrandWorker.VAR_ASSIGNED_USER_ID
	})
public class RtjUpdateErrandWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_TITLE = "title";
	static final String VAR_STATUS = "status";
	static final String VAR_PRIORITY = "priority";
	static final String VAR_DESCRIPTION = "description";
	static final String VAR_REPORTER_USER_ID = "reporterUserId";
	static final String VAR_ASSIGNED_USER_ID = "assignedUserId";

	private static final Logger LOG = LoggerFactory.getLogger(RtjUpdateErrandWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public RtjUpdateErrandWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-update-errand.cron:*/5 * * * * *}", name = "rtj-update-errand-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var patch = new PatchErrand();
		optionalVariable(task, VAR_TITLE, String.class).ifPresent(patch::title);
		optionalVariable(task, VAR_STATUS, String.class).ifPresent(patch::status);
		optionalVariable(task, VAR_PRIORITY, String.class).ifPresent(patch::priority);
		optionalVariable(task, VAR_DESCRIPTION, String.class).ifPresent(patch::description);
		optionalVariable(task, VAR_REPORTER_USER_ID, String.class).ifPresent(patch::reporterUserId);
		optionalVariable(task, VAR_ASSIGNED_USER_ID, String.class).ifPresent(patch::assignedUserId);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		rtjManagementClient.updateErrand(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			patch);

		LOG.info("Errand updated: {}", errandId);
		return emptyOutput();
	}
}
