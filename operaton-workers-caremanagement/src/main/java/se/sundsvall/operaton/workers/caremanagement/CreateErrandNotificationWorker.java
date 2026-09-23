package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Errand;
import generated.se.sundsvall.caremanagement.Notification;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.NonRetryableTaskException;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Raises a notification on an errand for its assigned caseworker in CareManagement, where Draken shows it — and, since
 * notifications are shared, to the errand's co-caseworkers too. Used when a process needs a person to act, e.g. a
 * bifall
 * whose payments have waited past their deadline.
 *
 * <p>
 * The recipient is the errand's {@code assignedUserId}, read from CareManagement when the task runs, so a reassignment
 * while the process waited is honoured. An errand nobody is assigned to cannot be notified; that is raised as an
 * incident at once instead of being retried, so it shows in Cockpit rather than vanishing.
 * </p>
 */
@Component
@TopicWorker(
	topic = "create-errand-notification",
	description = "Raises a notification on an errand for its assigned caseworker (and co-caseworkers) in CareManagement, shown in Draken. The errand is errandId or, when not mapped, the business key. An unassigned errand becomes an incident.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateErrandNotificationWorker.VAR_NAMESPACE,
		CreateErrandNotificationWorker.VAR_ERRAND_ID,
		CreateErrandNotificationWorker.VAR_DESCRIPTION,
		CreateErrandNotificationWorker.VAR_CONTENT,
		CreateErrandNotificationWorker.VAR_SUB_TYPE
	})
public class CreateErrandNotificationWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_DESCRIPTION = "notificationDescription";
	static final String VAR_CONTENT = "notificationContent";
	static final String VAR_SUB_TYPE = "notificationSubType";

	static final String TYPE = "CREATE";
	static final String DEFAULT_SUB_TYPE = "SYSTEM";
	static final String CREATED_BY = "Rakel";

	private static final Logger LOG = LoggerFactory.getLogger(CreateErrandNotificationWorker.class);

	private final CareManagementClient careManagementClient;

	public CreateErrandNotificationWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.create-errand-notification.cron:*/5 * * * * *}", name = "create-errand-notification-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var namespace = requireVariable(task, VAR_NAMESPACE, String.class);
		final var errandId = optionalVariable(task, VAR_ERRAND_ID, String.class)
			.or(() -> ofNullable(task.getBusinessKey()))
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new NonRetryableTaskException("No errandId and no business key on task %s".formatted(task.getId())));
		final var description = requireVariable(task, VAR_DESCRIPTION, String.class);

		final var ownerId = ofNullable(careManagementClient.readErrand(municipalityId, namespace, errandId))
			.map(ResponseEntity::getBody)
			.map(Errand::getAssignedUserId)
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new NonRetryableTaskException("Errand %s has no assigned caseworker to notify".formatted(errandId)));

		final var notification = new Notification()
			.ownerId(ownerId)
			.createdBy(CREATED_BY)
			.type(TYPE)
			.subType(optionalVariable(task, VAR_SUB_TYPE, String.class).orElse(DEFAULT_SUB_TYPE))
			.description(description);
		optionalVariable(task, VAR_CONTENT, String.class).ifPresent(notification::content);

		careManagementClient.createNotification(municipalityId, namespace, errandId, notification);

		LOG.info("Notification raised on errand {}", sanitizeForLogging(errandId));
		return emptyOutput();
	}
}
