package se.sundsvall.operaton.workers.framework;

import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

/**
 * Example external task worker that logs a message from a process variable. Demonstrates the self-documenting worker
 * pattern.
 */
@Component
@TopicWorker(
	topic = "log-message",
	description = "Logs a message from a process variable",
	inputVariables = {
		LoggerWorker.VAR_MESSAGE
	})
public class LoggerWorker extends AbstractTopicWorker {

	static final String VAR_MESSAGE = "message";

	private static final Logger LOG = LoggerFactory.getLogger(LoggerWorker.class);

	public LoggerWorker(final ExternalTaskService externalTaskService) {
		super(externalTaskService);
	}

	@Dept44Scheduled(cron = "${scheduler.logger-worker.cron:*/5 * * * * *}", name = "log-message-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var message = task.getVariables().get(VAR_MESSAGE);
		LOG.info("LoggerWorker received message: {}", message);
		return emptyOutput();
	}
}
