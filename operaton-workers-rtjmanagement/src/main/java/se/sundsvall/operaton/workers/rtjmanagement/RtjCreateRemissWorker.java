package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Remiss;
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
 * Sends a remiss/samråd on an errand in RtjManagement — e.g. requests a yttrande from miljökontor,
 * polis or länsstyrelse (14 § FBE) before an LBE permit is decided. The process is later resumed when
 * the remissvar is correlated back as a message.
 */
@Component
@TopicWorker(
	topic = "rtj-create-remiss",
	description = "Sends a remiss/samråd on an errand in RtjManagement (e.g. yttrande from miljökontor, polis or länsstyrelse inför ett LBE-tillstånd).",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		RtjCreateRemissWorker.VAR_NAMESPACE,
		RtjCreateRemissWorker.VAR_ERRAND_ID,
		RtjCreateRemissWorker.VAR_INSTANS,
		RtjCreateRemissWorker.VAR_RECIPIENT
	})
public class RtjCreateRemissWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_INSTANS = "instans";
	static final String VAR_RECIPIENT = "recipient";

	private static final Logger LOG = LoggerFactory.getLogger(RtjCreateRemissWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public RtjCreateRemissWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-create-remiss.cron:*/5 * * * * *}", name = "rtj-create-remiss-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var remiss = new Remiss().instans(requireVariable(task, VAR_INSTANS, String.class));
		optionalVariable(task, VAR_RECIPIENT, String.class).ifPresent(remiss::recipient);

		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		rtjManagementClient.createRemiss(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId,
			remiss);

		LOG.info("Remiss (instans {}) created on errand {}", sanitizeForLogging(remiss.getInstans()), sanitizeForLogging(errandId));
		return emptyOutput();
	}
}
