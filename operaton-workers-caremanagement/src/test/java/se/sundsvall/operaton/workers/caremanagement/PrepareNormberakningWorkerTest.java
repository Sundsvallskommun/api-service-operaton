package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareNormberakningWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private PrepareNormberakningWorker worker;

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "prepare-normberakning-worker");
	}

	@Test
	void handlePreparesAndOutputsCompletenessWithoutLifecareWrite() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06")
			.putValue("errandId", "cb20c51f-fcf3-42c0-b613-de563634a8ec")
			.putValue("classifiedIncomes", "[{\"normberakning\":\"Bostadsbidrag\"}]"));
		when(careManagementClientMock.prepareNormberakning(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new NormberakningResponse().informationComplete(false).missingIncomeTypes(List.of("Dagersättning"))));

		final var result = worker.handle(task);

		final var requestCaptor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).prepareNormberakning(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().getApplicant()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
		assertThat(requestCaptor.getValue().getErrandId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");
		assertThat(result).isEqualTo(Map.of(
			"informationComplete", false,
			"missingIncomeTypes", "Dagersättning",
			"normberakningHasWarnings", true));
	}

	@Test
	void handleDefaultsToCompleteWhenResponseBodyAbsent() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "199001011234")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.prepareNormberakning(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		assertThat(worker.handle(task)).isEqualTo(Map.of(
			"informationComplete", true,
			"missingIncomeTypes", "",
			"normberakningHasWarnings", false));
	}
}
