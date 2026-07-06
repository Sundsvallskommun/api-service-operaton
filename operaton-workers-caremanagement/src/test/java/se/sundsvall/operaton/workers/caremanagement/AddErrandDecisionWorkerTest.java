package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Decision;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddErrandDecisionWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private AddErrandDecisionWorker addErrandDecisionWorker;

	@Test
	void executePollsForTasks() {
		addErrandDecisionWorker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "add-errand-decision-worker");
	}

	@Test
	void handleWithAllVariables() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "errand-123")
			.putValue("decisionType", "RECOMMENDATION")
			.putValue("value", "Beslutsförslag: 7900 kr")
			.putValue("description", "Inom gränsvärde, ingen varning")
			.putValue("createdBy", "operaton"));
		when(careManagementClientMock.createErrandDecision(any(), any(), any(), any())).thenReturn(ResponseEntity.created(null).build());

		final var result = addErrandDecisionWorker.handle(task);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(careManagementClientMock).createErrandDecision(eq("2281"), eq("my-namespace"), eq("errand-123"), decisionCaptor.capture());

		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("RECOMMENDATION");
		assertThat(decision.getValue()).isEqualTo("Beslutsförslag: 7900 kr");
		assertThat(decision.getDescription()).isEqualTo("Inom gränsvärde, ingen varning");
		assertThat(decision.getCreatedBy()).isEqualTo("operaton");
		assertThat(result).isEqualTo(Map.of());
	}

	@Test
	void handleWithOnlyRequiredVariables() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "errand-999")
			.putValue("decisionType", "PAYMENT")
			.putValue("value", "APPROVED"));
		when(careManagementClientMock.createErrandDecision(any(), any(), any(), any())).thenReturn(ResponseEntity.created(null).build());

		final var result = addErrandDecisionWorker.handle(task);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(careManagementClientMock).createErrandDecision(eq("2281"), eq("my-namespace"), eq("errand-999"), decisionCaptor.capture());

		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("PAYMENT");
		assertThat(decision.getValue()).isEqualTo("APPROVED");
		assertThat(decision.getDescription()).isNull();
		assertThat(decision.getCreatedBy()).isNull();
		assertThat(result).isEqualTo(Map.of());
	}

	@Test
	void handleThrowsWhenDecisionTypeMissing() {
		final var task = mock(LockedExternalTask.class);
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> addErrandDecisionWorker.handle(task))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'decisionType' is missing on task task-1");
	}
}
