package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchFinancialAidBasisWorkerTest {

	private static final String WORKER_ID = "fetch-financial-aid-basis-worker";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FinancialAidClient financialAidClientMock;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private FetchFinancialAidBasisWorker worker;

	@Test
	void executeWithTasks() throws Exception {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		final Map<String, Map<String, Object>> response = Map.of(
			"af", Map.of("status", "approved", "amount", 7900),
			"csn", Map.of("loan", "active"));

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(response);

		worker.execute();

		verify(financialAidClientMock).getFinancialAidBasis("2281", "199001011234", "2026-01-01", "2026-05-20");

		final var outputCaptor = ArgumentCaptor.forClass(Map.class);
		verify(externalTaskServiceMock).complete(eq("task-1"), eq(WORKER_ID), outputCaptor.capture());

		final var output = outputCaptor.getValue();
		assertThat(output).containsOnlyKeys("financialAidBasis");
		final var roundTrip = objectMapper.readValue(
			(String) output.get("financialAidBasis"),
			new TypeReference<LinkedHashMap<String, LinkedHashMap<String, Object>>>() {});
		assertThat(roundTrip).isEqualTo(response);
	}

	@Test
	void executeWithEmptyResponse() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-2");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(Map.of());

		worker.execute();

		verify(externalTaskServiceMock).complete("task-2", WORKER_ID, Map.of("financialAidBasis", "{}"));
	}

	@Test
	void executeWithNullResponse() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-3");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(null);

		worker.execute();

		verify(externalTaskServiceMock).complete("task-3", WORKER_ID, Map.of("financialAidBasis", "{}"));
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		worker.execute();

		verifyNoInteractions(financialAidClientMock);
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

	@Test
	void executeWhenFinancialAidCallFails() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-4");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any()))
			.thenThrow(new RuntimeException("financial-aid 500"));

		worker.execute();

		verify(externalTaskServiceMock).handleFailure("task-4", WORKER_ID, "financial-aid 500", 0, 0L);
	}
}
