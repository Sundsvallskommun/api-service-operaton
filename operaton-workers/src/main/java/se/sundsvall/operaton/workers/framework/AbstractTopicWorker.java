package se.sundsvall.operaton.workers.framework;

import java.util.Map;
import java.util.Optional;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

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
	 * Read a required process variable of the expected type. Throws if the variable is missing or has a different type —
	 * both conditions are bugs in the workflow definition and surface as BPMN incidents via {@link #processTasks()}.
	 */
	protected static <T> T requireVariable(final LockedExternalTask task, final String name, final Class<T> type) {
		return optionalVariable(task, name, type)
			.orElseThrow(() -> new IllegalStateException(
				"Required process variable '%s' is missing on task %s".formatted(name, task.getId())));
	}

	/**
	 * Read an optional process variable of the expected type. Empty if the variable is absent. Throws if it is present but
	 * of a different type than expected.
	 */
	protected static <T> Optional<T> optionalVariable(final LockedExternalTask task, final String name, final Class<T> type) {
		return ofNullable(task.getVariables().get(name))
			.map(value -> {
				if (!type.isInstance(value)) {
					throw new IllegalStateException(
						"Process variable '%s' on task %s expected to be %s but was %s".formatted(
							name, task.getId(), type.getSimpleName(), value.getClass().getSimpleName()));
				}
				return type.cast(value);
			});
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
