package se.sundsvall.operaton.process.service;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Nightly demo reset: deletes every process instance (running, then historic) so the egensotning
 * demo starts from a clean slate each morning.
 *
 * Deployments and process definitions are deliberately left untouched — this scheduler never uses
 * {@code RepositoryService}. The BPMN process is deployed manually and would not auto-redeploy, so
 * wiping it would break the demo until someone re-uploaded it.
 */
@Component
public class ProcessCleanupScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessCleanupScheduler.class);

	private static final String DELETE_REASON = "nightly demo reset";

	private final RuntimeService runtimeService;
	private final HistoryService historyService;

	public ProcessCleanupScheduler(final RuntimeService runtimeService, final HistoryService historyService) {
		this.runtimeService = runtimeService;
		this.historyService = historyService;
	}

	@Dept44Scheduled(cron = "${scheduler.process-cleanup.cron:0 0 3 * * *}",
		name = "process-cleanup",
		lockAtMostFor = "PT5M")
	public void resetDemoProcesses() {
		deleteRunningInstances();
		deleteHistoricInstances();
	}

	private void deleteRunningInstances() {
		final var runningIds = runtimeService.createProcessInstanceQuery().list().stream()
			.map(ProcessInstance::getId)
			.toList();
		if (!runningIds.isEmpty()) {
			// skipCustomListeners + externallyTerminated: an admin-driven reset must not fire process
			// listeners or look like a normal completion.
			runtimeService.deleteProcessInstances(runningIds, DELETE_REASON, true, true);
		}
		LOG.info("Nightly demo reset deleted {} running process instance(s)", runningIds.size());
	}

	private void deleteHistoricInstances() {
		final var historicIds = historyService.createHistoricProcessInstanceQuery().list().stream()
			.map(HistoricProcessInstance::getId)
			.toList();
		if (!historicIds.isEmpty()) {
			// Bulk + empty-guard: the non-bulk variant throws on an empty/unknown id list.
			historyService.deleteHistoricProcessInstancesBulk(historicIds);
		}
		LOG.info("Nightly demo reset deleted {} historic process instance(s)", historicIds.size());
	}
}
