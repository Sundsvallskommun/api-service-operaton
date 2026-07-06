package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.RpaTaskRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchLifecareSupplementsWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private FetchLifecareSupplementsWorker worker;

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "fetch-lifecare-supplements-worker");
	}

	@Test
	void handleEnqueuesRpaFetch() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "f47ac10b-58cc-4372-a567-0e02b2c3d479"));

		final var result = worker.handle(task);

		verify(careManagementClientMock).enqueueRpaTask(eq("2281"), eq("my-namespace"), eq("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
			eq(new RpaTaskRequest().action("FETCH_SUPPLEMENTS")));
		assertThat(result).isEmpty();
	}
}
