package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Decision;
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
class RtjAddErrandDecisionWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private RtjManagementClient rtjManagementClientMock;

	@InjectMocks
	private RtjAddErrandDecisionWorker addErrandDecisionWorker;

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
			.putValue("decisionType", "RECOMMENDATION")
			.putValue("value", "Beslutsförslag: 7900 kr")
			.putValue("description", "Inom gränsvärde, ingen varning")
			.putValue("createdBy", "operaton"));
		when(rtjManagementClientMock.createErrandDecision(any(), any(), any(), any())).thenReturn(ResponseEntity.created(null).build());

		addErrandDecisionWorker.execute();

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(rtjManagementClientMock).createErrandDecision(eq("2281"), eq("my-namespace"), eq("errand-123"), decisionCaptor.capture());
		verify(externalTaskServiceMock).complete("task-1", "rtj-add-errand-decision-worker", Map.of());

		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("RECOMMENDATION");
		assertThat(decision.getValue()).isEqualTo("Beslutsförslag: 7900 kr");
		assertThat(decision.getDescription()).isEqualTo("Inom gränsvärde, ingen varning");
		assertThat(decision.getCreatedBy()).isEqualTo("operaton");
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
			.putValue("decisionType", "PAYMENT")
			.putValue("value", "APPROVED"));
		when(rtjManagementClientMock.createErrandDecision(any(), any(), any(), any())).thenReturn(ResponseEntity.created(null).build());

		addErrandDecisionWorker.execute();

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(rtjManagementClientMock).createErrandDecision(eq("2281"), eq("my-namespace"), eq("errand-999"), decisionCaptor.capture());

		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("PAYMENT");
		assertThat(decision.getValue()).isEqualTo("APPROVED");
		assertThat(decision.getDescription()).isNull();
		assertThat(decision.getCreatedBy()).isNull();
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		addErrandDecisionWorker.execute();
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

		addErrandDecisionWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", "rtj-add-errand-decision-worker", "test error", 0, 0L);
	}

	@Test
	void executeWhenRtjManagementCallFails() {
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
			.putValue("decisionType", "PAYMENT")
			.putValue("value", "APPROVED"));
		when(rtjManagementClientMock.createErrandDecision(any(), any(), any(), any()))
			.thenThrow(new RuntimeException("careM 500"));

		addErrandDecisionWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-3", "rtj-add-errand-decision-worker", "careM 500", 0, 0L);
	}
}
