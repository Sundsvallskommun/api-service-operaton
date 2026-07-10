package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.RpaTaskRequest;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

/**
 * Triggers the fetch of the Lifecare-only supplements an EB återansökan needs but SSBTEK does not carry — bevakningar /
 * notiser, journal entries and documents — so the handläggare sees the full picture in Draken alongside the
 * normberäkning.
 *
 * <p>
 * Enqueues an RPA <em>fetch</em> task via CareManagement ({@code POST .../errands/{errandId}/rpa-tasks}, action
 * {@code FETCH_SUPPLEMENTS}). A UiPath robot reads the supplements out of Lifecare and writes them back onto the errand
 * through the existing CareManagement journal/document/monitoring endpoints — so nothing flows back through this
 * worker;
 * it only kicks off the robot and completes. Failures propagate so the engine retries the trigger.
 * </p>
 */
@Component
@TopicWorker(
	topic = "fetch-lifecare-supplements",
	description = "Enqueues an RPA fetch (action FETCH_SUPPLEMENTS) via CareManagement for the Lifecare-only supplements (bevakningar/notiser, journal, dokument) SSBTEK does not carry. A robot fetches them and writes them back onto the errand via the CareManagement endpoints; this worker only triggers and completes.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		FetchLifecareSupplementsWorker.VAR_NAMESPACE,
		FetchLifecareSupplementsWorker.VAR_ERRAND_ID
	})
public class FetchLifecareSupplementsWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";

	private static final String ACTION_FETCH_SUPPLEMENTS = "FETCH_SUPPLEMENTS";

	private static final Logger LOG = LoggerFactory.getLogger(FetchLifecareSupplementsWorker.class);

	private final CareManagementClient careManagementClient;

	public FetchLifecareSupplementsWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.fetch-lifecare-supplements.cron:*/5 * * * * *}", name = "fetch-lifecare-supplements-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var namespace = requireVariable(task, VAR_NAMESPACE, String.class);
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);

		careManagementClient.enqueueRpaTask(municipalityId, namespace, errandId, new RpaTaskRequest().action(ACTION_FETCH_SUPPLEMENTS));

		LOG.info("Enqueued RPA fetch of Lifecare supplements for errand {}", errandId);
		return Map.of();
	}
}
