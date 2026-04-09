package se.sundsvall.operaton.integration.worker;

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
import se.sundsvall.operaton.integration.supportmanagement.SupportManagementClient;
import se.sundsvall.operaton.integration.worker.annotation.TopicWorker;

import static java.util.Optional.ofNullable;

@Component
@TopicWorker(
	topic = "create-errand",
	description = "Creates an errand in SupportManagement",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateErrandWorker.VAR_NAMESPACE,
		CreateErrandWorker.VAR_TITLE,
		CreateErrandWorker.VAR_PRIORITY,
		CreateErrandWorker.VAR_STATUS,
		CreateErrandWorker.VAR_REPORTER_USER_ID,
		CreateErrandWorker.VAR_CATEGORY,
		CreateErrandWorker.VAR_TYPE,
		CreateErrandWorker.VAR_DESCRIPTION
	},
	outputVariables = {
		CreateErrandWorker.VAR_ERRAND_ID
	})
public class CreateErrandWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_TITLE = "title";
	static final String VAR_PRIORITY = "priority";
	static final String VAR_STATUS = "status";
	static final String VAR_REPORTER_USER_ID = "reporterUserId";
	static final String VAR_CATEGORY = "category";
	static final String VAR_TYPE = "type";
	static final String VAR_DESCRIPTION = "description";
	static final String VAR_ERRAND_ID = "errandId";

	private static final Logger LOG = LoggerFactory.getLogger(CreateErrandWorker.class);
	private static final String DEFAULT_PRIORITY = "MEDIUM";
	private static final String DEFAULT_STATUS = "NEW";
	private static final String UNKNOWN_ERRAND_ID = "unknown";

	private final SupportManagementClient supportManagementClient;

	public CreateErrandWorker(final ExternalTaskService externalTaskService, final SupportManagementClient supportManagementClient) {
		super(externalTaskService);
		this.supportManagementClient = supportManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.create-errand.cron:*/5 * * * * *}", name = "create-errand-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errand = new Errand()
			.title(stringVar(task, VAR_TITLE))
			.priority(Errand.PriorityEnum.fromValue(optionalStringVar(task, VAR_PRIORITY).orElse(DEFAULT_PRIORITY)))
			.status(optionalStringVar(task, VAR_STATUS).orElse(DEFAULT_STATUS))
			.reporterUserId(stringVar(task, VAR_REPORTER_USER_ID))
			.description(stringVar(task, VAR_DESCRIPTION));

		optionalStringVar(task, VAR_CATEGORY)
			.ifPresent(category -> errand.classification(new Classification().category(category).type(stringVar(task, VAR_TYPE))));

		final var response = supportManagementClient.createErrand(
			stringVar(task, VAR_MUNICIPALITY_ID),
			stringVar(task, VAR_NAMESPACE),
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
