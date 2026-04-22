package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.PatchErrand;
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
	topic = "update-errand",
	description = "Updates an errand in CareManagement",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		UpdateErrandWorker.VAR_NAMESPACE,
		UpdateErrandWorker.VAR_ERRAND_ID,
		UpdateErrandWorker.VAR_TITLE,
		UpdateErrandWorker.VAR_CATEGORY,
		UpdateErrandWorker.VAR_TYPE,
		UpdateErrandWorker.VAR_STATUS,
		UpdateErrandWorker.VAR_PRIORITY,
		UpdateErrandWorker.VAR_DESCRIPTION,
		UpdateErrandWorker.VAR_REPORTER_USER_ID,
		UpdateErrandWorker.VAR_ASSIGNED_USER_ID,
		UpdateErrandWorker.VAR_CONTACT_REASON,
		UpdateErrandWorker.VAR_CONTACT_REASON_DESCRIPTION
	})
public class UpdateErrandWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_TITLE = "title";
	static final String VAR_CATEGORY = "category";
	static final String VAR_TYPE = "type";
	static final String VAR_STATUS = "status";
	static final String VAR_PRIORITY = "priority";
	static final String VAR_DESCRIPTION = "description";
	static final String VAR_REPORTER_USER_ID = "reporterUserId";
	static final String VAR_ASSIGNED_USER_ID = "assignedUserId";
	static final String VAR_CONTACT_REASON = "contactReason";
	static final String VAR_CONTACT_REASON_DESCRIPTION = "contactReasonDescription";

	private static final Logger LOG = LoggerFactory.getLogger(UpdateErrandWorker.class);

	private final CareManagementClient careManagementClient;

	public UpdateErrandWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.update-errand.cron:*/5 * * * * *}", name = "update-errand-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var patch = new PatchErrand();
		optionalVariable(task, VAR_TITLE, String.class).ifPresent(patch::title);
		optionalVariable(task, VAR_CATEGORY, String.class).ifPresent(patch::category);
		optionalVariable(task, VAR_TYPE, String.class).ifPresent(patch::type);
		optionalVariable(task, VAR_STATUS, String.class).ifPresent(patch::status);
		optionalVariable(task, VAR_PRIORITY, String.class).ifPresent(patch::priority);
		optionalVariable(task, VAR_DESCRIPTION, String.class).ifPresent(patch::description);
		optionalVariable(task, VAR_REPORTER_USER_ID, String.class).ifPresent(patch::reporterUserId);
		optionalVariable(task, VAR_ASSIGNED_USER_ID, String.class).ifPresent(patch::assignedUserId);
		optionalVariable(task, VAR_CONTACT_REASON, String.class).ifPresent(patch::contactReason);
		optionalVariable(task, VAR_CONTACT_REASON_DESCRIPTION, String.class).ifPresent(patch::contactReasonDescription);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		careManagementClient.updateErrand(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			patch);

		LOG.info("Errand updated: {}", errandId);
		return emptyOutput();
	}
}
