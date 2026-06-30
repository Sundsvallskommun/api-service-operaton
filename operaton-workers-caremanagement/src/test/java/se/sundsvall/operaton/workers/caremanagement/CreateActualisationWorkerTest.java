package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.ActualisationRequest;
import generated.se.sundsvall.caremanagement.ActualisationResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;
import org.springframework.http.ResponseEntity;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateActualisationWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CreateActualisationWorker worker;

	@Test
	void executeWithTask() {
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
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06")
			.putValue("errandId", "cb20c51f-fcf3-42c0-b613-de563634a8ec"));
		when(careManagementClientMock.createActualisation(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new ActualisationResponse().actualisationId(8842)));

		worker.execute();

		final var requestCaptor = ArgumentCaptor.forClass(ActualisationRequest.class);
		verify(careManagementClientMock).createActualisation(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		final var request = requestCaptor.getValue();
		assertThat(request.getApplicant()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
		assertThat(request.getErrandId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");

		verify(externalTaskServiceMock).complete("task-1", "create-actualisation-worker", Map.of("actualisationId", 8842));
	}

	@Test
	void executeWithNullResponseBody() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-2");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.createActualisation(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		worker.execute();

		verify(externalTaskServiceMock).complete("task-2", "create-actualisation-worker", emptyMap());
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		worker.execute();
	}

	@Test
	void executeWithMissingApplicant() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-3");
		when(task.getVariables()).thenReturn(Variables.createVariables());

		worker.execute();

		verify(externalTaskServiceMock).handleFailure("task-3", "create-actualisation-worker",
			"Required process variable 'applicant' is missing on task task-3", 0, 0L);
	}

	@Test
	void executeWhenCareManagementCallFails() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-4");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.createActualisation(any(), any(), any())).thenThrow(new RuntimeException("careM 500"));

		worker.execute();

		verify(externalTaskServiceMock).handleFailure("task-4", "create-actualisation-worker", "careM 500", 0, 0L);
	}
}
