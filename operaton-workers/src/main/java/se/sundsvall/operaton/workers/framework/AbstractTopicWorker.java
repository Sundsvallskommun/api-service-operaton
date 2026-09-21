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

	// Retry policy: a downstream outage (gateway restart, token endpoint 503) must not wedge every in-flight instance on
	// the first failure. Doubling from 15s over 5 attempts rides out roughly 3m45s of downtime before an incident.
	private static final int MAX_ATTEMPTS = 5;
	private static final long INITIAL_RETRY_BACKOFF_MS = 15_000L;
	private static final long MAX_RETRY_BACKOFF_MS = 300_000L;

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
			.orElseThrow(() -> new NonRetryableTaskException(
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
					throw new NonRetryableTaskException(
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
	 * Whether this is the last attempt before the engine raises an incident — i.e. failing now leaves no retries. Lets a
	 * worker ride out a transient downstream outage on the normal backoff ladder and only then degrade to a
	 * business-level outcome, instead of reporting the first hiccup as a real answer. A task that has never failed
	 * carries no retry count and is therefore never the final attempt.
	 */
	protected static boolean isFinalAttempt(final LockedExternalTask task) {
		return ofNullable(task.getRetries()).filter(retries -> retries <= 1).isPresent();
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
				handleFailure(task, e);
			}
		});
	}

	/**
	 * Fail a task with an exponential backoff instead of going straight to an incident, so a transient downstream outage
	 * is ridden out rather than wedging the process instance. Retries count down from {@link #maxAttempts()} and the
	 * engine raises the incident only once they reach zero. Failures that can never succeed on a retry
	 * ({@link NonRetryableTaskException}) skip the backoff entirely.
	 */
	private void handleFailure(final LockedExternalTask task, final Exception e) {
		final var remainingRetries = e instanceof NonRetryableTaskException ? 0 : remainingRetries(task);
		final var backoffMs = remainingRetries == 0 ? 0L : backoffMs(maxAttempts() - remainingRetries);

		if (remainingRetries == 0) {
			LOG.error("{} failed to process task {}, giving up - the engine will raise an incident", workerId, task.getId(), e);
		} else {
			LOG.warn("{} failed to process task {}, retrying in {} ms ({} attempt(s) left)", workerId, task.getId(), backoffMs, remainingRetries, e);
		}
		externalTaskService.handleFailure(task.getId(), workerId, e.getMessage(), remainingRetries, backoffMs);
	}

	/**
	 * Retries left after this failure. A task that has never failed carries no retry count, so this failure is the first
	 * of {@link #maxAttempts()}.
	 */
	private int remainingRetries(final LockedExternalTask task) {
		return ofNullable(task.getRetries())
			.map(retries -> Math.max(retries - 1, 0))
			.orElseGet(() -> maxAttempts() - 1);
	}

	/**
	 * Backoff before the next attempt, doubling per failure up to a cap. {@code failureCount} is 1 on the first failure.
	 */
	private static long backoffMs(final int failureCount) {
		// Clamped because retries may have been raised by hand in Cockpit, which puts failureCount outside 1..maxAttempts.
		return Math.min(INITIAL_RETRY_BACKOFF_MS << Math.clamp(failureCount - 1L, 0, 16), MAX_RETRY_BACKOFF_MS);
	}

	/** Total attempts (initial try plus retries) before the engine raises an incident. Override to tune per worker. */
	protected int maxAttempts() {
		return MAX_ATTEMPTS;
	}

	/**
	 * Process a single locked task. Return a map of variables to set on completion, or an empty map if the worker produces
	 * no output variables. Use {@link #emptyOutput()} for readability.
	 */
	protected abstract Map<String, Object> handle(LockedExternalTask task);
}
