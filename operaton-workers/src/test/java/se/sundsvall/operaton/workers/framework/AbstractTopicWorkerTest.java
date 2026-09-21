package se.sundsvall.operaton.workers.framework;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.operaton.workers.framework.AbstractTopicWorker.optionalVariable;
import static se.sundsvall.operaton.workers.framework.AbstractTopicWorker.requireVariable;

@ExtendWith(MockitoExtension.class)
class AbstractTopicWorkerTest {

	@Mock
	private LockedExternalTask taskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Test
	void requireVariableReturnsValueWhenPresentAndCorrectType() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("title", "Hello"));

		assertThat(requireVariable(taskMock, "title", String.class)).isEqualTo("Hello");
	}

	@Test
	void requireVariableThrowsWhenMissing() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> requireVariable(taskMock, "title", String.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'title' is missing on task task-1");
	}

	@Test
	void requireVariableThrowsWhenWrongType() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("title", 42));

		assertThatThrownBy(() -> requireVariable(taskMock, "title", String.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Process variable 'title' on task task-1 expected to be String but was Integer");
	}

	@Test
	void optionalVariableReturnsEmptyWhenMissing() {
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables());

		assertThat(optionalVariable(taskMock, "title", String.class)).isEmpty();
	}

	@Test
	void optionalVariableReturnsValueWhenPresentAndCorrectType() {
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("title", "Hello"));

		assertThat(optionalVariable(taskMock, "title", String.class)).contains("Hello");
	}

	@Test
	void optionalVariableThrowsWhenWrongType() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("count", "not-an-integer"));

		assertThatThrownBy(() -> optionalVariable(taskMock, "count", Integer.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Process variable 'count' on task task-1 expected to be Integer but was String");
	}

	@Test
	void processTasksCompletesEachTaskWithHandleOutput() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		final var output = Map.<String, Object>of("out", "value");

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");

		new TestWorker(externalTaskServiceMock, _ -> output).processTasks();

		verify(externalTaskServiceMock).complete("task-1", "test-topic-worker", output);
	}

	@Test
	void processTasksCallsHandleFailureWithBackoffWhenHandleThrows() {
		final var task = taskFailingWith(null);

		new TestWorker(externalTaskServiceMock, _ -> {
			throw new RuntimeException("boom");
		}).processTasks();

		// First failure of five attempts: four left, first backoff step.
		verify(externalTaskServiceMock).handleFailure(task.getId(), "test-topic-worker", "boom", 4, 15_000L);
	}

	@ParameterizedTest
	@CsvSource({
		// retries carried by the task, retries left after this failure, backoff before the next attempt
		"4, 3, 30000",
		"3, 2, 60000",
		"2, 1, 120000",
	})
	void processTasksBacksOffExponentiallyAcrossFailures(final int retries, final int expectedRetriesLeft, final long expectedBackoffMs) {
		final var task = taskFailingWith(retries);

		new TestWorker(externalTaskServiceMock, _ -> {
			throw new RuntimeException("boom");
		}).processTasks();

		verify(externalTaskServiceMock).handleFailure(task.getId(), "test-topic-worker", "boom", expectedRetriesLeft, expectedBackoffMs);
	}

	@Test
	void processTasksRaisesIncidentWhenRetriesAreExhausted() {
		final var task = taskFailingWith(1);

		new TestWorker(externalTaskServiceMock, _ -> {
			throw new RuntimeException("boom");
		}).processTasks();

		verify(externalTaskServiceMock).handleFailure(task.getId(), "test-topic-worker", "boom", 0, 0L);
	}

	@Test
	void processTasksCapsTheBackoffWhenRetriesWereRaisedByHand() {
		// Retries set above maxAttempts in Cockpit must not produce a negative shift or an unbounded wait.
		final var task = taskFailingWith(50);

		new TestWorker(externalTaskServiceMock, _ -> {
			throw new RuntimeException("boom");
		}).processTasks();

		verify(externalTaskServiceMock).handleFailure(task.getId(), "test-topic-worker", "boom", 49, 15_000L);
	}

	@Test
	void processTasksSkipsTheBackoffForNonRetryableFailures() {
		final var task = taskFailingWith(null);

		new TestWorker(externalTaskServiceMock, _ -> {
			throw new NonRetryableTaskException("bad model");
		}).processTasks();

		verify(externalTaskServiceMock).handleFailure(task.getId(), "test-topic-worker", "bad model", 0, 0L);
	}

	@Test
	void processTasksHonoursAMaxAttemptsOverride() {
		final var task = taskFailingWith(null);

		new TestWorker(externalTaskServiceMock, _ -> {
			throw new RuntimeException("boom");
		}, 2).processTasks();

		verify(externalTaskServiceMock).handleFailure(task.getId(), "test-topic-worker", "boom", 1, 15_000L);
	}

	/** Stub a single fetched task carrying {@code retries} (null = never failed before). */
	private LockedExternalTask taskFailingWith(final Integer retries) {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-2");
		// Not read on the non-retryable path, which short-circuits before consulting the retry count.
		lenient().when(task.getRetries()).thenReturn(retries);

		return task;
	}

	@Test
	void processTasksWithNoTasksDoesNothing() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		new TestWorker(externalTaskServiceMock, _ -> Map.of()).processTasks();

		verify(externalTaskServiceMock, never()).complete(any(), any(), any());
		verify(externalTaskServiceMock, never()).handleFailure(any(), any(), any(), anyInt(), anyLong());
	}

	@TopicWorker(topic = "test-topic")
	private static final class TestWorker extends AbstractTopicWorker {

		private final Function<LockedExternalTask, Map<String, Object>> handler;
		private final int maxAttempts;

		private TestWorker(final ExternalTaskService externalTaskService, final Function<LockedExternalTask, Map<String, Object>> handler) {
			this(externalTaskService, handler, 5);
		}

		private TestWorker(final ExternalTaskService externalTaskService, final Function<LockedExternalTask, Map<String, Object>> handler, final int maxAttempts) {
			super(externalTaskService);
			this.handler = handler;
			this.maxAttempts = maxAttempts;
		}

		@Override
		protected int maxAttempts() {
			return maxAttempts;
		}

		@Override
		protected Map<String, Object> handle(final LockedExternalTask task) {
			return handler.apply(task);
		}
	}
}
