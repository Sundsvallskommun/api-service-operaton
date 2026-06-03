package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Remiss;
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
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RtjCreateRemissWorkerTest {

	private static final String WORKER_ID = "rtj-create-remiss-worker";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private RtjManagementClient rtjManagementClientMock;

	@InjectMocks
	private RtjCreateRemissWorker worker;

	private void stubbedTask(final VariableMap variables) {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(variables);
	}

	@Test
	void executeCreatesRemissWithInstansAndRecipient() {
		stubbedTask(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "BRANDFARLIG_VARA")
			.putValue("errandId", "errand-123")
			.putValue("instans", "MILJOKONTOR")
			.putValue("recipient", "miljokontoret@sundsvall.se"));

		worker.execute();

		final var captor = ArgumentCaptor.forClass(Remiss.class);
		verify(rtjManagementClientMock).createRemiss(eq("2281"), eq("BRANDFARLIG_VARA"), eq("errand-123"), captor.capture());
		assertThat(captor.getValue().getInstans()).isEqualTo("MILJOKONTOR");
		assertThat(captor.getValue().getRecipient()).isEqualTo("miljokontoret@sundsvall.se");
		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, of());
	}

	@Test
	void executeCreatesRemissWithoutRecipient() {
		stubbedTask(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "EXPLOSIV_VARA")
			.putValue("errandId", "errand-9")
			.putValue("instans", "POLIS"));

		worker.execute();

		final var captor = ArgumentCaptor.forClass(Remiss.class);
		verify(rtjManagementClientMock).createRemiss(eq("2281"), eq("EXPLOSIV_VARA"), eq("errand-9"), captor.capture());
		assertThat(captor.getValue().getInstans()).isEqualTo("POLIS");
		assertThat(captor.getValue().getRecipient()).isNull();
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

		worker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", WORKER_ID, "test error", 0, 0L);
	}
}
