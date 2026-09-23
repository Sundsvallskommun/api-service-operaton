package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Errand;
import generated.se.sundsvall.caremanagement.Notification;
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
import se.sundsvall.operaton.workers.framework.NonRetryableTaskException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateErrandNotificationWorkerTest {

	private static final String ERRAND_ID = "a3c1f4de-2b6a-4c1e-9d3f-7e8a9b0c1d2e";

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CreateErrandNotificationWorker worker;

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "create-errand-notification-worker");
	}

	@Test
	void handleNotifiesTheAssignedCaseworkerOfTheBusinessKeyErrand() {
		final var task = mock(LockedExternalTask.class);
		when(task.getBusinessKey()).thenReturn(ERRAND_ID);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "FINANCIAL_ASSISTANCE")
			.putValue("notificationDescription", "Utbetalningen är inte bekräftad i Lifecare")
			.putValue("notificationContent", "1 av 1 beslutade utbetalningar är inte registrerade i Lifecare"));
		when(careManagementClientMock.readErrand("2281", "FINANCIAL_ASSISTANCE", ERRAND_ID)).thenReturn(ResponseEntity.ok(new Errand().assignedUserId("jan01han")));

		assertThat(worker.handle(task)).isEmpty();

		final var captor = ArgumentCaptor.forClass(Notification.class);
		verify(careManagementClientMock).createNotification(eq("2281"), eq("FINANCIAL_ASSISTANCE"), eq(ERRAND_ID), captor.capture());
		assertThat(captor.getValue().getOwnerId()).isEqualTo("jan01han");
		assertThat(captor.getValue().getCreatedBy()).isEqualTo("Rakel");
		assertThat(captor.getValue().getType()).isEqualTo("CREATE");
		assertThat(captor.getValue().getSubType()).isEqualTo("SYSTEM");
		assertThat(captor.getValue().getDescription()).isEqualTo("Utbetalningen är inte bekräftad i Lifecare");
		assertThat(captor.getValue().getContent()).isEqualTo("1 av 1 beslutade utbetalningar är inte registrerade i Lifecare");
	}

	@Test
	void handlePrefersTheErrandIdVariableAndTheGivenSubType() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "FINANCIAL_ASSISTANCE")
			.putValue("errandId", ERRAND_ID)
			.putValue("notificationSubType", "DECISION")
			.putValue("notificationDescription", "Beslut behöver åtgärdas"));
		when(careManagementClientMock.readErrand("2281", "FINANCIAL_ASSISTANCE", ERRAND_ID)).thenReturn(ResponseEntity.ok(new Errand().assignedUserId("jan01han")));

		worker.handle(task);

		final var captor = ArgumentCaptor.forClass(Notification.class);
		verify(careManagementClientMock).createNotification(eq("2281"), eq("FINANCIAL_ASSISTANCE"), eq(ERRAND_ID), captor.capture());
		assertThat(captor.getValue().getSubType()).isEqualTo("DECISION");
		assertThat(captor.getValue().getContent()).isNull();
	}

	@Test
	void handleRaisesAnIncidentWhenNobodyIsAssigned() {
		final var task = mock(LockedExternalTask.class);
		when(task.getBusinessKey()).thenReturn(ERRAND_ID);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "FINANCIAL_ASSISTANCE")
			.putValue("notificationDescription", "Utbetalningen är inte bekräftad i Lifecare"));
		when(careManagementClientMock.readErrand("2281", "FINANCIAL_ASSISTANCE", ERRAND_ID)).thenReturn(ResponseEntity.ok(new Errand().assignedUserId(" ")));

		assertThatThrownBy(() -> worker.handle(task))
			.isInstanceOf(NonRetryableTaskException.class)
			.hasMessage("Errand " + ERRAND_ID + " has no assigned caseworker to notify");
		verify(careManagementClientMock, never()).createNotification(any(), any(), any(), any());
	}

	@Test
	void handleRaisesAnIncidentWithoutAnyErrand() {
		final var task = mock(LockedExternalTask.class);
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "FINANCIAL_ASSISTANCE"));

		assertThatThrownBy(() -> worker.handle(task))
			.isInstanceOf(NonRetryableTaskException.class)
			.hasMessage("No errandId and no business key on task task-1");
	}
}
