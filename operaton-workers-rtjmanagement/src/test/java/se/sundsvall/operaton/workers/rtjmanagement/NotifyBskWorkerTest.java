package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Stakeholder;
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
class NotifyBskWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private RtjManagementClient rtjManagementClientMock;

	@InjectMocks
	private NotifyBskWorker notifyBskWorker;

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
			.putValue("errandId", "errand-123"));
		when(rtjManagementClientMock.createErrandStakeholder(any(), any(), any(), any())).thenReturn(ResponseEntity.noContent().build());

		notifyBskWorker.execute();

		final var stakeholderCaptor = ArgumentCaptor.forClass(Stakeholder.class);
		verify(rtjManagementClientMock).createErrandStakeholder(eq("2281"), eq("my-namespace"), eq("errand-123"), stakeholderCaptor.capture());
		verify(externalTaskServiceMock).complete("task-1", "rtj-notify-bsk-worker", Map.of());

		final var stakeholder = stakeholderCaptor.getValue();
		assertThat(stakeholder.getRole()).isEqualTo("BSK");
		assertThat(stakeholder.getFirstName()).isEqualTo("BSK");
		assertThat(stakeholder.getLastName()).isEqualTo("Handläggare");
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

		notifyBskWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", "rtj-notify-bsk-worker", "test error", 0, 0L);
	}
}
