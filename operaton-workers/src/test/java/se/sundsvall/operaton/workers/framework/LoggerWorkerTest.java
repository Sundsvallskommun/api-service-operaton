package se.sundsvall.operaton.workers.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggerWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@InjectMocks
	private LoggerWorker loggerWorker;

	@Test
	void executePollsForTasks() {
		loggerWorker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "log-message-worker");
	}

	@Test
	void handleLogsMessageAndReturnsEmptyOutput() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables().putValue("message", "hello"));

		assertThat(loggerWorker.handle(task)).isEmpty();
	}
}
