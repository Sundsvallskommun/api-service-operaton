package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.PatchErrand;
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
class UpdateErrandWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private UpdateErrandWorker updateErrandWorker;

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
			.putValue("title", "Updated title")
			.putValue("status", "ONGOING")
			.putValue("priority", "HIGH")
			.putValue("assignedUserId", "jane02doe")
			.putValue("contactReason", "PHONE"));
		when(careManagementClientMock.updateErrand(any(), any(), any(), any())).thenReturn(ResponseEntity.noContent().build());

		updateErrandWorker.execute();

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(careManagementClientMock).updateErrand(eq("2281"), eq("my-namespace"), eq("errand-123"), patchCaptor.capture());
		verify(externalTaskServiceMock).complete(eq("task-1"), eq("update-errand-worker"), eq(Map.of()));

		final var patch = patchCaptor.getValue();
		assertThat(patch.getTitle()).isEqualTo("Updated title");
		assertThat(patch.getStatus()).isEqualTo("ONGOING");
		assertThat(patch.getPriority()).isEqualTo("HIGH");
		assertThat(patch.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(patch.getContactReason()).isEqualTo("PHONE");
		assertThat(patch.getCategory()).isNull();
		assertThat(patch.getType()).isNull();
		assertThat(patch.getDescription()).isNull();
		assertThat(patch.getReporterUserId()).isNull();
		assertThat(patch.getContactReasonDescription()).isNull();
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
			.putValue("errandId", "errand-999"));
		when(careManagementClientMock.updateErrand(any(), any(), any(), any())).thenReturn(ResponseEntity.noContent().build());

		updateErrandWorker.execute();

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(careManagementClientMock).updateErrand(eq("2281"), eq("my-namespace"), eq("errand-999"), patchCaptor.capture());

		final var patch = patchCaptor.getValue();
		assertThat(patch.getTitle()).isNull();
		assertThat(patch.getStatus()).isNull();
		assertThat(patch.getPriority()).isNull();
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		updateErrandWorker.execute();
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

		updateErrandWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", "update-errand-worker", "test error", 0, 0L);
	}
}
