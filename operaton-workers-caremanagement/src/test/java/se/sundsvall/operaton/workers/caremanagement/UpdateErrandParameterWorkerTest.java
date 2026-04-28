package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Parameter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateErrandParameterWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private UpdateErrandParameterWorker updateErrandParameterWorker;

	@Test
	void executeWithTasks() {
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
			.putValue("errandId", "errand-123")
			.putValue("parameterKey", "recommendation")
			.putValue("parameterValue", "Beslutsförslag: 7900 kr — Ingen varning")
			.putValue("parameterDisplayName", "Beslutsförslag från regelverk")
			.putValue("parameterGroup", "decision"));
		when(careManagementClientMock.createErrandParameter(any(), any(), any(), any())).thenReturn(ResponseEntity.created(null).build());

		updateErrandParameterWorker.execute();

		final var parameterCaptor = ArgumentCaptor.forClass(Parameter.class);
		verify(careManagementClientMock).createErrandParameter(eq("2281"), eq("my-namespace"), eq("errand-123"), parameterCaptor.capture());
		verify(externalTaskServiceMock).complete("task-1", "update-errand-parameter-worker", Map.of());

		final var parameter = parameterCaptor.getValue();
		assertThat(parameter.getKey()).isEqualTo("recommendation");
		assertThat(parameter.getValues()).containsExactly("Beslutsförslag: 7900 kr — Ingen varning");
		assertThat(parameter.getDisplayName()).isEqualTo("Beslutsförslag från regelverk");
		assertThat(parameter.getParameterGroup()).isEqualTo("decision");
	}

	@Test
	void executeWithOnlyRequiredVariables() {
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
			.putValue("errandId", "errand-999")
			.putValue("parameterKey", "decisionWarning")
			.putValue("parameterValue", "Ingen varning"));
		when(careManagementClientMock.createErrandParameter(any(), any(), any(), any())).thenReturn(ResponseEntity.created(null).build());

		updateErrandParameterWorker.execute();

		final var parameterCaptor = ArgumentCaptor.forClass(Parameter.class);
		verify(careManagementClientMock).createErrandParameter(eq("2281"), eq("my-namespace"), eq("errand-999"), parameterCaptor.capture());

		final var parameter = parameterCaptor.getValue();
		assertThat(parameter.getKey()).isEqualTo("decisionWarning");
		assertThat(parameter.getValues()).containsExactly("Ingen varning");
		assertThat(parameter.getDisplayName()).isNull();
		assertThat(parameter.getParameterGroup()).isNull();
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		updateErrandParameterWorker.execute();
	}

	@Test
	void executeWithException() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenThrow(new RuntimeException("test error"));

		updateErrandParameterWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", "update-errand-parameter-worker", "test error", 0, 0L);
	}

	@Test
	void executeWhenCareManagementCallFails() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-3");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "errand-3")
			.putValue("parameterKey", "k")
			.putValue("parameterValue", "v"));
		when(careManagementClientMock.createErrandParameter(any(), any(), any(), any()))
			.thenThrow(new RuntimeException("careM 500"));

		updateErrandParameterWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-3", "update-errand-parameter-worker", "careM 500", 0, 0L);
	}
}
