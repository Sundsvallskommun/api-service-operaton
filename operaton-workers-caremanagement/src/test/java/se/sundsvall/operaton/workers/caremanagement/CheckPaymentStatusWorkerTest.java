package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.PaymentStatusRequest;
import generated.se.sundsvall.caremanagement.PaymentStatusResponse;
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
class CheckPaymentStatusWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CheckPaymentStatusWorker worker;

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "check-payment-status-worker");
	}

	@Test
	void handleEffectuated() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.checkPaymentStatus(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new PaymentStatusResponse().effectuated(true).paymentDate("2026-05-27")));

		final var result = worker.handle(task);

		final var requestCaptor = ArgumentCaptor.forClass(PaymentStatusRequest.class);
		verify(careManagementClientMock).checkPaymentStatus(eq("2281"), eq("my-namespace"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().getApplicant()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
		assertThat(requestCaptor.getValue().getApplicationMonth()).isEqualTo("2026-06");
		assertThat(result).isEqualTo(Map.of("paymentEffectuated", true));
	}

	@Test
	void handleNotEffectuated() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.checkPaymentStatus(any(), any(), any())).thenReturn(
			ResponseEntity.ok(new PaymentStatusResponse().effectuated(false)));

		assertThat(worker.handle(task)).isEqualTo(Map.of("paymentEffectuated", false));
	}

	@Test
	void handleNullResponseBody() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("applicant", "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.putValue("applicationMonth", "2026-06"));
		when(careManagementClientMock.checkPaymentStatus(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		assertThat(worker.handle(task)).isEqualTo(Map.of("paymentEffectuated", false));
	}

	@Test
	void handleThrowsWhenApplicantMissing() {
		final var task = mock(LockedExternalTask.class);
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> worker.handle(task))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'applicant' is missing on task task-1");
	}
}
