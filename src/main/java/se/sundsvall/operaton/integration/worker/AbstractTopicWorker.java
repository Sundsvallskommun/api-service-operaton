package se.sundsvall.operaton.integration.worker;

import java.util.Map;
import java.util.Optional;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import se.sundsvall.operaton.integration.worker.annotation.TopicWorker;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

/**
 * Base class for external task workers. Handles the fetch-lock-complete-or-fail boilerplate so subclasses only need to
 * implement the actual task logic in {@link #handle(LockedExternalTask)}. Topic name and worker id are derived from the
 * {@link TopicWorker} annotation on the subclass.
 */
public abstract class AbstractTopicWorker {

	/** Common input variable used by every worker that calls a municipality-scoped API. */
	public static final String VAR_MUNICIPALITY_ID = "municipalityId";

	private static final Logger LOG = LoggerFactory.getLogger(AbstractTopicWorker.class);
	private static final int MAX_TASKS = 10;
	private static final long LOCK_DURATION_MS = 60_000L;

	protected final ExternalTaskService externalTaskService;
	private final String topic;
	private final String workerId;

	protected AbstractTopicWorker(final ExternalTaskService externalTaskService) {
		this.externalTaskService = externalTaskService;
		final var annotation = AnnotationUtils.findAnnotation(getClass(), TopicWorker.class);
		if (annotation == null) {
			throw new IllegalStateException("%s must be annotated with @TopicWorker".formatted(getClass().getSimpleName()));
		}
		this.topic = annotation.topic();
		this.workerId = topic + "-worker";
	}

	/**
	 * Read a string-typed process variable from the task. Returns {@code null} if the variable is absent.
	 */
	protected static String stringVar(final LockedExternalTask task, final String name) {
		return (String) task.getVariables().get(name);
	}

	/**
	 * Read an optional string-typed process variable from the task. Use when you need to provide a default or skip
	 * downstream logic when the variable is missing.
	 */
	protected static Optional<String> optionalStringVar(final LockedExternalTask task, final String name) {
		return ofNullable(stringVar(task, name));
	}

	protected static Map<String, Object> emptyOutput() {
		return emptyMap();
	}

	/**
	 * Poll for tasks on this worker's topic, invoke {@link #handle(LockedExternalTask)} for each, and complete or fail the
	 * task based on the result. Called from the subclass's scheduled method.
	 */
	protected final void processTasks() {
		final var tasks = externalTaskService.fetchAndLock(MAX_TASKS, workerId)
			.topic(topic, LOCK_DURATION_MS)
			.execute();

		tasks.forEach(task -> {
			try {
				externalTaskService.complete(task.getId(), workerId, handle(task));
			} catch (final Exception e) {
				LOG.error("{} failed to process task {}", workerId, task.getId(), e);
				externalTaskService.handleFailure(task.getId(), workerId, e.getMessage(), 0, 0);
			}
		});
	}

	/**
	 * Process a single locked task. Return a map of variables to set on completion, or an empty map if the worker produces
	 * no output variables. Use {@link #emptyOutput()} for readability.
	 */
	protected abstract Map<String, Object> handle(LockedExternalTask task);
}
