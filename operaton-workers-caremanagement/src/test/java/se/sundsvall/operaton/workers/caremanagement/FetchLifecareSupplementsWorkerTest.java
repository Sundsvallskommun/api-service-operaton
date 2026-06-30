package se.sundsvall.operaton.workers.caremanagement;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchLifecareSupplementsWorkerTest {

	private static final String WORKER_ID = "fetch-lifecare-supplements-worker";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private FetchLifecareSupplementsWorker worker;

	@Test
	void executeEnqueuesRpaFetchAndCompletes() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "f47ac10b-58cc-4372-a567-0e02b2c3d479"));

		worker.execute();

		verify(careManagementClientMock).enqueueRpaTask(eq("2281"), eq("my-namespace"), eq("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
			eq(Map.of("action", "FETCH_SUPPLEMENTS")));
		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, Map.of());
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		worker.execute();

		verify(externalTaskServiceMock, never()).complete(any(), any(), any());
	}
}
