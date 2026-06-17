package se.sundsvall.operaton.workers.caremanagement;

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
 * normberäkning. In the target design this kicks off an RPA (or a Lifecare integration) that writes the supplements
 * back
 * onto the errand.
 *
 * <p>
 * <strong>STUB:</strong> no RPA trigger or Lifecare bevakningar/journal integration exists yet, so this worker only
 * completes its task (logging that it ran) to keep the process flowing. Wire it to the real downstream call once that
 * capability lands; the BPMN step and topic are already in place.
 * </p>
 */
@Component
@TopicWorker(
	topic = "fetch-lifecare-supplements",
	description = "STUB — triggers the fetch of Lifecare-only supplements (bevakningar/notiser, journal, dokument) for an EB errand, the data SSBTEK does not carry. No RPA/Lifecare integration exists yet, so it just completes the task so the process flows; wire to the real downstream call when available.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		FetchLifecareSupplementsWorker.VAR_NAMESPACE,
		FetchLifecareSupplementsWorker.VAR_ERRAND_ID
	})
public class FetchLifecareSupplementsWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";

	private static final Logger LOG = LoggerFactory.getLogger(FetchLifecareSupplementsWorker.class);

	public FetchLifecareSupplementsWorker(final ExternalTaskService externalTaskService) {
		super(externalTaskService);
	}

	@Dept44Scheduled(cron = "${scheduler.fetch-lifecare-supplements.cron:*/5 * * * * *}", name = "fetch-lifecare-supplements-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		// STUB: no RPA/Lifecare bevakningar-journal integration yet — complete the task so the process flows.
		LOG.info("Lifecare supplements fetch triggered (stub — no downstream call yet)");
		return Map.of();
	}
}
