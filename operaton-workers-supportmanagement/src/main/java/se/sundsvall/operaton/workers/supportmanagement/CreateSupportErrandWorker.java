package se.sundsvall.operaton.workers.supportmanagement;

import generated.se.sundsvall.supportmanagement.Classification;
import generated.se.sundsvall.supportmanagement.Errand;
import java.net.URI;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.util.Optional.ofNullable;

@Component
@TopicWorker(
	topic = "create-support-errand",
	description = "Creates an errand in SupportManagement",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateSupportErrandWorker.VAR_NAMESPACE,
		CreateSupportErrandWorker.VAR_TITLE,
		CreateSupportErrandWorker.VAR_PRIORITY,
		CreateSupportErrandWorker.VAR_STATUS,
		CreateSupportErrandWorker.VAR_REPORTER_USER_ID,
		CreateSupportErrandWorker.VAR_CATEGORY,
		CreateSupportErrandWorker.VAR_TYPE,
		CreateSupportErrandWorker.VAR_DESCRIPTION
	},
	outputVariables = {
		CreateSupportErrandWorker.VAR_ERRAND_ID
	})
public class CreateSupportErrandWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_TITLE = "title";
	static final String VAR_PRIORITY = "priority";
	static final String VAR_STATUS = "status";
	static final String VAR_REPORTER_USER_ID = "reporterUserId";
	static final String VAR_CATEGORY = "category";
	static final String VAR_TYPE = "type";
	static final String VAR_DESCRIPTION = "description";
	static final String VAR_ERRAND_ID = "errandId";

	private static final Logger LOG = LoggerFactory.getLogger(CreateSupportErrandWorker.class);
	private static final String DEFAULT_PRIORITY = "MEDIUM";
	private static final String DEFAULT_STATUS = "NEW";
	private static final String UNKNOWN_ERRAND_ID = "unknown";

	private final SupportManagementClient supportManagementClient;

	public CreateSupportErrandWorker(final ExternalTaskService externalTaskService, final SupportManagementClient supportManagementClient) {
		super(externalTaskService);
		this.supportManagementClient = supportManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.create-support-errand.cron:*/5 * * * * *}", name = "create-support-errand-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errand = new Errand()
			.title(requireVariable(task, VAR_TITLE, String.class))
			.priority(Errand.PriorityEnum.fromValue(optionalVariable(task, VAR_PRIORITY, String.class).orElse(DEFAULT_PRIORITY)))
			.status(optionalVariable(task, VAR_STATUS, String.class).orElse(DEFAULT_STATUS))
			.reporterUserId(requireVariable(task, VAR_REPORTER_USER_ID, String.class))
			.description(requireVariable(task, VAR_DESCRIPTION, String.class));

		optionalVariable(task, VAR_CATEGORY, String.class)
			.ifPresent(category -> errand.classification(new Classification().category(category).type(requireVariable(task, VAR_TYPE, String.class))));

		final var response = supportManagementClient.createErrand(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errand);

		final var errandId = extractErrandId(response);
		LOG.info("Errand created with id {}", errandId);
		return Map.of(VAR_ERRAND_ID, errandId);
	}

	private static String extractErrandId(final ResponseEntity<Void> response) {
		return ofNullable(response)
			.map(ResponseEntity::getHeaders)
			.map(HttpHeaders::getLocation)
			.map(URI::getPath)
			.map(path -> path.substring(path.lastIndexOf('/') + 1))
			.orElse(UNKNOWN_ERRAND_ID);
	}
}
