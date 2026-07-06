package se.sundsvall.operaton.workers.supportmanagement;

import generated.se.sundsvall.supportmanagement.Errand;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
	private SupportManagementClient supportManagementClientMock;

	@InjectMocks
	private CreateErrandWorker createErrandWorker;

	@Test
	void executePollsForTasks() {
		createErrandWorker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "create-errand-worker");
	}

	@Test
	void handleCreatesErrand() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("priority", "HIGH")
			.putValue("status", "NEW")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description"));
		when(supportManagementClientMock.createErrand(any(), any(), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2281/my-namespace/errands/errand-123")).build());

		final var result = createErrandWorker.handle(task);

		final var errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(supportManagementClientMock).createErrand(eq("2281"), eq("my-namespace"), errandCaptor.capture());
		final var errand = errandCaptor.getValue();
		assertThat(errand.getTitle()).isEqualTo("Test errand");
		assertThat(errand.getPriority()).isEqualTo(Errand.PriorityEnum.HIGH);
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getReporterUserId()).isEqualTo("user-1");
		assertThat(errand.getDescription()).isEqualTo("Test description");
		assertThat(errand.getClassification()).isNull();
		assertThat(result).isEqualTo(Map.of("errandId", "errand-123"));
	}

	@Test
	void handleAppliesDefaultsAndClassification() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description")
			.putValue("category", "CATEGORY_1")
			.putValue("type", "TYPE_1"));
		when(supportManagementClientMock.createErrand(any(), any(), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2281/my-namespace/errands/errand-456")).build());

		final var result = createErrandWorker.handle(task);

		final var errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(supportManagementClientMock).createErrand(eq("2281"), eq("my-namespace"), errandCaptor.capture());
		final var errand = errandCaptor.getValue();
		assertThat(errand.getPriority()).isEqualTo(Errand.PriorityEnum.MEDIUM);
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getClassification()).isNotNull();
		assertThat(errand.getClassification().getCategory()).isEqualTo("CATEGORY_1");
		assertThat(errand.getClassification().getType()).isEqualTo("TYPE_1");
		assertThat(result).isEqualTo(Map.of("errandId", "errand-456"));
	}

	@Test
	void handleReturnsUnknownErrandIdWhenLocationMissing() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "my-namespace")
			.putValue("title", "Test errand")
			.putValue("reporterUserId", "user-1")
			.putValue("description", "Test description"));
		when(supportManagementClientMock.createErrand(any(), any(), any())).thenReturn(ResponseEntity.ok().build());

		assertThat(createErrandWorker.handle(task)).isEqualTo(Map.of("errandId", "unknown"));
	}

	@Test
	void handleThrowsWhenTitleMissing() {
		final var task = mock(LockedExternalTask.class);
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> createErrandWorker.handle(task))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'title' is missing on task task-1");
	}
}
