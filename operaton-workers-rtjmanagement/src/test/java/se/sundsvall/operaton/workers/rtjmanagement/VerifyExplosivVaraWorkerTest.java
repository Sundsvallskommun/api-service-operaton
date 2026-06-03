package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.ExplosivVaraVerificationResult;
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
class VerifyExplosivVaraWorkerTest {

	private static final String WORKER_ID = "rtj-verify-explosiv-vara-worker";
	private static final String DECISION_TEXT = "Tillstånd till hantering av explosiv vara beviljas. ...";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private RtjManagementClient rtjManagementClientMock;

	@InjectMocks
	private VerifyExplosivVaraWorker worker;

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
			.putValue("namespace", "EXPLOSIV_VARA")
			.putValue("errandId", "errand-123"));
	}

	@Test
	void executeNeedsSupplementSetsSupplementReason() {
		stubbedTaskWithVariables();
		when(rtjManagementClientMock.verifyExplosivVara("2281", "EXPLOSIV_VARA", "errand-123"))
			.thenReturn(new ExplosivVaraVerificationResult()
				.outcome("NEEDS_SUPPLEMENT")
				.bilagaPresent(false)
				.productsPresent(true)
				.supplementReason("bilaga (t.ex. riskutredning, situationsplan)")
				.decisionDescription(DECISION_TEXT));

		worker.execute();

		final Map<String, Object> expected = new HashMap<>();
		expected.put("outcome", "NEEDS_SUPPLEMENT");
		expected.put("bilagaPresent", false);
		expected.put("productsPresent", true);
		expected.put("supplementReason", "bilaga (t.ex. riskutredning, situationsplan)");
		expected.put("decisionDescription", DECISION_TEXT);
		verify(rtjManagementClientMock).verifyExplosivVara("2281", "EXPLOSIV_VARA", "errand-123");
		verify(externalTaskServiceMock).complete("task-1", WORKER_ID, expected);
	}

	@Test
	void executeManualReviewOmitsSupplementReason() {
		stubbedTaskWithVariables();
		when(rtjManagementClientMock.verifyExplosivVara("2281", "EXPLOSIV_VARA", "errand-123"))
			.thenReturn(new ExplosivVaraVerificationResult()
				.outcome("NEEDS_MANUAL_REVIEW")
				.bilagaPresent(true)
				.productsPresent(true)
				.decisionDescription(DECISION_TEXT));

		worker.execute();

		final Map<String, Object> expected = new HashMap<>();
		expected.put("outcome", "NEEDS_MANUAL_REVIEW");
		expected.put("bilagaPresent", true);
		expected.put("productsPresent", true);
		expected.put("decisionDescription", DECISION_TEXT);
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
