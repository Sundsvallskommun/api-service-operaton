package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Errand;
import java.net.URI;
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
class CreateErrandWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@InjectMocks
	private CreateErrandWorker createErrandWorker;

	@Test
	void executePollsForTasks() {
		createErrandWorker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "create-errand-worker");
	}

	@Test
	void handleWithAllVariables() {
		final var task = mock(LockedExternalTask.class);
		final ResponseEntity<Void> responseEntity = ResponseEntity.created(URI.create("/2281/my-namespace/errands/errand-123")).build();
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("priority", "HIGH")
			.putValue("status", "NEW")
			.putValue("reporterUserId", "user-1")
			.putValue("category", "CATEGORY-1")
			.putValue("type", "TYPE-1")
			.putValue("description", "Test description"));
		when(careManagementClientMock.createErrand(any(), any(), any())).thenReturn(responseEntity);

		final var result = createErrandWorker.handle(task);

		final var errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(careManagementClientMock).createErrand(eq("2281"), eq("my-namespace"), errandCaptor.capture());
		assertThat(result).isEqualTo(Map.of("errandId", "errand-123"));

		final var errand = errandCaptor.getValue();
		assertThat(errand.getTitle()).isEqualTo("Test errand");
		assertThat(errand.getPriority()).isEqualTo("HIGH");
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getReporterUserId()).isEqualTo("user-1");
		assertThat(errand.getCategory()).isEqualTo("CATEGORY-1");
		assertThat(errand.getType()).isEqualTo("TYPE-1");
		assertThat(errand.getDescription()).isEqualTo("Test description");
	}

	@Test
	void handleUsesDefaultsWhenPriorityAndStatusAbsent() {
		final var task = mock(LockedExternalTask.class);
		final ResponseEntity<Void> responseEntity = ResponseEntity.created(URI.create("/2281/my-namespace/errands/errand-456")).build();
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description"));
		when(careManagementClientMock.createErrand(any(), any(), any())).thenReturn(responseEntity);

		createErrandWorker.handle(task);

		final var errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(careManagementClientMock).createErrand(eq("2281"), eq("my-namespace"), errandCaptor.capture());

		final var errand = errandCaptor.getValue();
		assertThat(errand.getPriority()).isEqualTo("MEDIUM");
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getCategory()).isNull();
		assertThat(errand.getType()).isNull();
	}

	@Test
	void handleWithMissingLocationHeaderFallsBackToUnknown() {
		final var task = mock(LockedExternalTask.class);
		final ResponseEntity<Void> responseEntity = ResponseEntity.<Void>noContent().build();
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description"));
		when(careManagementClientMock.createErrand(any(), any(), any())).thenReturn(responseEntity);

		assertThat(createErrandWorker.handle(task)).isEqualTo(Map.of("errandId", "unknown"));
	}
}
