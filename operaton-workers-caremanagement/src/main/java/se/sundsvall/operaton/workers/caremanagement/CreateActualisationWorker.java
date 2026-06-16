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

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Creates the Lifecare actualisation (aktualisering) and archives the application for an ekonomiskt-bistånd errand.
 *
 * <p>
 * STUB: there is no CareManagement/Lifecare actualisation endpoint yet, so the worker currently only completes the task
 * so the rakel-ekonomiskt-bistand process can proceed. When the endpoint exists, inject {@link CareManagementClient}
 * and
 * create the actualisation + archive the application (incl. attachments) here, mirroring
 * {@link CreateNormberakningWorker}.
 */
@Component
@TopicWorker(
	topic = "create-actualisation",
	description = "Creates the Lifecare actualisation (aktualisering) and archives the application for an ekonomiskt-bistånd errand, via CareManagement. STUB: no CareManagement/Lifecare actualisation endpoint exists yet — the worker only completes the task so the process can proceed; wire it to the real CareManagement call once the endpoint is available.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateActualisationWorker.VAR_NAMESPACE,
		CreateActualisationWorker.VAR_ERRAND_ID,
		CreateActualisationWorker.VAR_APPLICANT,
		CreateActualisationWorker.VAR_APPLICATION_MONTH
	})
public class CreateActualisationWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";

	private static final Logger LOG = LoggerFactory.getLogger(CreateActualisationWorker.class);

	public CreateActualisationWorker(final ExternalTaskService externalTaskService) {
		super(externalTaskService);
	}

	@Dept44Scheduled(cron = "${scheduler.create-actualisation.cron:*/5 * * * * *}", name = "create-actualisation-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);

		// STUB: when CareManagement exposes a Lifecare actualisation endpoint, create the
		// actualisation and archive the application (incl. attachments) here.

		LOG.info("Actualisation step completed for errand {}", sanitizeForLogging(errandId));
		return emptyOutput();
	}
}
