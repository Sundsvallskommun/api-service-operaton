package se.sundsvall.operaton.integration.worker;

import org.operaton.bpm.engine.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.integration.worker.annotation.TopicWorker;

/**
 * Example external task worker that logs a message from a process variable. Demonstrates the self-documenting worker
 * pattern.
 */
@Component
@TopicWorker(
	topic = "log-message",
	description = "Logs a message from a process variable",
	inputVariables = {
		"message"
	},
	outputVariables = {})
public class LoggerWorker {

	private static final Logger LOG = LoggerFactory.getLogger(LoggerWorker.class);
	private static final String TOPIC = "log-message";
	private static final String WORKER_ID = "logger-worker";

	private final ExternalTaskService externalTaskService;

	public LoggerWorker(final ExternalTaskService externalTaskService) {
		this.externalTaskService = externalTaskService;
	}

	@Dept44Scheduled(cron = "${scheduler.logger-worker.cron:*/5 * * * * *}", name = WORKER_ID, lockAtMostFor = "PT30S")
	public void execute() {
		final var tasks = externalTaskService.fetchAndLock(10, WORKER_ID)
			.topic(TOPIC, 60000)
			.execute();

		for (final var task : tasks) {
			try {
				final var message = task.getVariables().get("message");
				LOG.info("LoggerWorker received message: {}", message);
				externalTaskService.complete(task.getId(), WORKER_ID);
			} catch (final Exception e) {
				LOG.error("LoggerWorker failed to process task {}", task.getId(), e);
				externalTaskService.handleFailure(task.getId(), WORKER_ID, e.getMessage(), 0, 0);
			}
		}
	}
}
