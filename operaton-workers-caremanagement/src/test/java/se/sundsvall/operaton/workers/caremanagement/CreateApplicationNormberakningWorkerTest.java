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
class CreateApplicationNormberakningWorkerTest {

	private static final String WORKER_ID = "create-application-normberakning-worker";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CreateApplicationNormberakningWorker worker;

	@Test
	void executeCreatesFromApplicationAndOutputsCalculationId() {
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
			.putValue("errandId", "errand-1"));
		when(careManagementClientMock.createApplicationNormberakning(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new NormberakningResponse().calculationId(5001)));

		worker.execute();

		final var requestCaptor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).createApplicationNormberakning(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().getApplicant()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
		assertThat(requestCaptor.getValue().getCoApplicant()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
		assertThat(requestCaptor.getValue().getErrandId()).isEqualTo("errand-1");
		assertThat(requestCaptor.getValue().getApplicationMonth()).isEqualTo("2026-06");

		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, Map.of("normberakningCalculationId", 5001));
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
		when(careManagementClientMock.createApplicationNormberakning(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		worker.execute();

		verify(externalTaskServiceMock).complete("task-2", WORKER_ID, Map.of());
	}
}
