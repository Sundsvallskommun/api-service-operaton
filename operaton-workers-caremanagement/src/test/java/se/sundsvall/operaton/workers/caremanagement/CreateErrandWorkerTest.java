package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Errand;
import java.net.URI;
import java.util.List;
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
class CreateErrandWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CreateErrandWorker createErrandWorker;

	@Test
	void executeWithTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		final ResponseEntity<Void> responseEntity = ResponseEntity.created(URI.create("/2281/my-namespace/errands/errand-123")).build();

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("priority", "HIGH")
			.putValue("status", "NEW")
			.putValue("reporterUserId", "user-1")
			.putValue("category", "CATEGORY-1")
			.putValue("type", "TYPE-1")
			.putValue("description", "Test description"));
		when(careManagementClientMock.createErrand(any(), any(), any())).thenReturn(responseEntity);

		createErrandWorker.execute();

		final var errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(careManagementClientMock).createErrand(eq("2281"), eq("my-namespace"), errandCaptor.capture());
		verify(externalTaskServiceMock).complete(eq("task-1"), eq("create-errand-worker"), eq(java.util.Map.of("errandId", "errand-123")));

		final var errand = errandCaptor.getValue();
		assertThat(errand.getTitle()).isEqualTo("Test errand");
		assertThat(errand.getPriority()).isEqualTo("HIGH");
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getReporterUserId()).isEqualTo("user-1");
		assertThat(errand.getCategory()).isEqualTo("CATEGORY-1");
		assertThat(errand.getType()).isEqualTo("TYPE-1");
		assertThat(errand.getDescription()).isEqualTo("Test description");
	}

	@Test
	void executeWithTasksUsesDefaultsWhenPriorityAndStatusAbsent() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		final ResponseEntity<Void> responseEntity = ResponseEntity.created(URI.create("/2281/my-namespace/errands/errand-456")).build();

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-2");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description"));
		when(careManagementClientMock.createErrand(any(), any(), any())).thenReturn(responseEntity);

		createErrandWorker.execute();

		final var errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(careManagementClientMock).createErrand(eq("2281"), eq("my-namespace"), errandCaptor.capture());

		final var errand = errandCaptor.getValue();
		assertThat(errand.getPriority()).isEqualTo("MEDIUM");
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getCategory()).isNull();
		assertThat(errand.getType()).isNull();
	}

	@Test
	void executeWithMissingLocationHeaderFallsBackToUnknown() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		final ResponseEntity<Void> responseEntity = ResponseEntity.<Void>noContent().build();

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-3");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description"));
		when(careManagementClientMock.createErrand(any(), any(), any())).thenReturn(responseEntity);

		createErrandWorker.execute();

		verify(externalTaskServiceMock).complete(eq("task-3"), eq("create-errand-worker"), eq(java.util.Map.of("errandId", "unknown")));
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		createErrandWorker.execute();
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

		createErrandWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", "create-errand-worker", "test error", 0, 0L);
	}
}
