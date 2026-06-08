package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.DocumentValidationResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateEgensotningDocumentsWorkerTest {

	private static final String WORKER_ID = "rtj-validate-egensotning-documents-worker";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private RtjManagementClient rtjManagementClientMock;

	@InjectMocks
	private ValidateEgensotningDocumentsWorker worker;

	private void stubbedTaskWithVariables() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("namespace", "EGENSOTNING")
			.putValue("errandId", "errand-123"));
	}

	@Test
	void executeValidWritesTrue() {
		stubbedTaskWithVariables();
		when(rtjManagementClientMock.validateDocuments("2281", "EGENSOTNING", "errand-123"))
			.thenReturn(new DocumentValidationResult().valid(true).documentTypeOk(true).identityMatch(true).reason("OK"));

		worker.execute();

		final Map<String, Object> expected = new HashMap<>();
		expected.put("documentsValid", true);
		expected.put("documentValidationReason", "OK");
		verify(rtjManagementClientMock).validateDocuments("2281", "EGENSOTNING", "errand-123");
		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, expected);
	}

	@Test
	void executeInvalidWritesFalse() {
		stubbedTaskWithVariables();
		when(rtjManagementClientMock.validateDocuments("2281", "EGENSOTNING", "errand-123"))
			.thenReturn(new DocumentValidationResult().valid(false).reason("Namn matchar inte"));

		worker.execute();

		final Map<String, Object> expected = new HashMap<>();
		expected.put("documentsValid", false);
		expected.put("documentValidationReason", "Namn matchar inte");
		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, expected);
	}

	@Test
	void executeValidWithoutReasonOmitsReasonVariable() {
		stubbedTaskWithVariables();
		when(rtjManagementClientMock.validateDocuments("2281", "EGENSOTNING", "errand-123"))
			.thenReturn(new DocumentValidationResult().valid(true));

		worker.execute();

		final Map<String, Object> expected = new HashMap<>();
		expected.put("documentsValid", true);
		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, expected);
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
