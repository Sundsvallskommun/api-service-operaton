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
class CreateNormberakningWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CreateNormberakningWorker worker;

	@Test
	void executeWithWarnings() {
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
			.putValue("applicant", "199001011234")
			.putValue("coApplicant", "199202022345")
			.putValue("errandId", "cb20c51f-fcf3-42c0-b613-de563634a8ec")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.createNormberakning(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new NormberakningResponse()
				.calculationId(4711)
				.unhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)"))
				.changeWarnings(List.of("Bostadsbidrag: -23%"))));

		worker.execute();

		final var requestCaptor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).createNormberakning(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		final var request = requestCaptor.getValue();
		assertThat(request.getApplicant()).isEqualTo("199001011234");
		assertThat(request.getCoApplicant()).isEqualTo("199202022345");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
		assertThat(request.getErrandId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");

		verify(externalTaskServiceMock).complete("task-1", "create-normberakning-worker", Map.of(
			"normberakningCalculationId", 4711,
			"normberakningHasWarnings", true,
			"normberakningUnhandledIncomes", "Bostadstillägg (NOT_ON_WHITELIST)",
			"normberakningChangeWarnings", "Bostadsbidrag: -23%"));
	}

	@Test
	void executePassesClassifiedIncomesAndSplitsWarnings() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-c");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "199001011234")
			.putValue("applicationMonth", "2026-06")
			.putValue("classifiedIncomes", "[{\"normberakning\":\"Bostadsbidrag\"}]")
			.putValue("unhandledIncomes", "Något (EJ_PA_LISTAN); Annat (EJ_PA_LISTAN)")
			.putValue("changeWarnings", "Bostadsbidrag: -23%"));
		when(careManagementClientMock.createNormberakning(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new NormberakningResponse().calculationId(4713)));

		worker.execute();

		final var requestCaptor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).createNormberakning(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		final var request = requestCaptor.getValue();
		assertThat(request.getClassifiedIncomes()).isEqualTo("[{\"normberakning\":\"Bostadsbidrag\"}]");
		assertThat(request.getUnhandledIncomes()).containsExactly("Något (EJ_PA_LISTAN)", "Annat (EJ_PA_LISTAN)");
		assertThat(request.getChangeWarnings()).containsExactly("Bostadsbidrag: -23%");
	}

	@Test
	void executeWithoutWarningsOrCoApplicant() {
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
		when(careManagementClientMock.createNormberakning(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new NormberakningResponse().calculationId(4712)));

		worker.execute();

		final var requestCaptor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).createNormberakning(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().getCoApplicant()).isNull();

		verify(externalTaskServiceMock).complete("task-2", "create-normberakning-worker", Map.of(
			"normberakningCalculationId", 4712,
			"normberakningHasWarnings", false,
			"normberakningUnhandledIncomes", "",
			"normberakningChangeWarnings", ""));
	}

	@Test
	void executeWithNullResponseBody() {
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
			.putValue("applicant", "199001011234")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.createNormberakning(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		worker.execute();

		// No calculation id when the body is absent; no warnings.
		verify(externalTaskServiceMock).complete("task-3", "create-normberakning-worker", Map.of(
			"normberakningHasWarnings", false,
			"normberakningUnhandledIncomes", "",
			"normberakningChangeWarnings", ""));
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
	void executeWithException() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-4");
		when(task.getVariables()).thenThrow(new RuntimeException("test error"));

		worker.execute();

		verify(externalTaskServiceMock).handleFailure("task-4", "create-normberakning-worker", "test error", 0, 0L);
	}

	@Test
	void executeWhenCareManagementCallFails() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-5");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "199001011234")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.createNormberakning(any(), any(), any())).thenThrow(new RuntimeException("careM 500"));

		worker.execute();

		verify(externalTaskServiceMock).handleFailure("task-5", "create-normberakning-worker", "careM 500", 0, 0L);
	}
}
