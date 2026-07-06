package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.PatchErrand;
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
class UpdateErrandWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private UpdateErrandWorker updateErrandWorker;

	@Test
	void executePollsForTasks() {
		updateErrandWorker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "update-errand-worker");
	}

	@Test
	void handleWithAllVariables() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "errand-123")
			.putValue("title", "Updated title")
			.putValue("status", "ONGOING")
			.putValue("priority", "HIGH")
			.putValue("assignedUserId", "jane02doe")
			.putValue("contactReason", "PHONE"));
		when(careManagementClientMock.updateErrand(any(), any(), any(), any())).thenReturn(ResponseEntity.noContent().build());

		final var result = updateErrandWorker.handle(task);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(careManagementClientMock).updateErrand(eq("2281"), eq("my-namespace"), eq("errand-123"), patchCaptor.capture());
		assertThat(result).isEqualTo(Map.of());

		final var patch = patchCaptor.getValue();
		assertThat(patch.getTitle()).isEqualTo("Updated title");
		assertThat(patch.getStatus()).isEqualTo("ONGOING");
		assertThat(patch.getPriority()).isEqualTo("HIGH");
		assertThat(patch.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(patch.getContactReason()).isEqualTo("PHONE");
		assertThat(patch.getCategory()).isNull();
		assertThat(patch.getType()).isNull();
		assertThat(patch.getDescription()).isNull();
		assertThat(patch.getReporterUserId()).isNull();
		assertThat(patch.getContactReasonDescription()).isNull();
	}

	@Test
	void handleWithOnlyRequiredVariables() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("errandId", "errand-999"));
		when(careManagementClientMock.updateErrand(any(), any(), any(), any())).thenReturn(ResponseEntity.noContent().build());

		final var result = updateErrandWorker.handle(task);

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(careManagementClientMock).updateErrand(eq("2281"), eq("my-namespace"), eq("errand-999"), patchCaptor.capture());
		assertThat(result).isEqualTo(Map.of());

		final var patch = patchCaptor.getValue();
		assertThat(patch.getTitle()).isNull();
		assertThat(patch.getStatus()).isNull();
		assertThat(patch.getPriority()).isNull();
	}
}
