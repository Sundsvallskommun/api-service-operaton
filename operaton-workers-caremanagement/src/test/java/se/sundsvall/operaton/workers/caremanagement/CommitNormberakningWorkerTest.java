package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
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
class CommitNormberakningWorkerTest {

	private static final String WORKER_ID = "commit-normberakning-worker";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CommitNormberakningWorker worker;

	@Test
	void executeCommitsToLifecareAndOutputsCalculationId() {
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
			.putValue("coApplicant", "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
			.putValue("applicationMonth", "2026-06")
			.putValue("classifiedIncomes", "[{\"normberakning\":\"Bostadsbidrag\"}]"));
		when(careManagementClientMock.commitNormberakning(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new NormberakningResponse().calculationId(4711)));

		worker.execute();

		final var requestCaptor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).commitNormberakning(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().getCoApplicant()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, Map.of("normberakningCalculationId", 4711));
	}

	@Test
	void executeWithNullBodyOutputsNothing() {
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
			.putValue("applicant", "199001011234")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.commitNormberakning(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		worker.execute();

		verify(externalTaskServiceMock).complete("task-2", WORKER_ID, Map.of());
	}
}
