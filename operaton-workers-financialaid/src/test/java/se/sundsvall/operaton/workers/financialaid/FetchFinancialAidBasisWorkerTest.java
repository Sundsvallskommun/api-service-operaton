package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchFinancialAidBasisWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FinancialAidClient financialAidClientMock;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private FetchFinancialAidBasisWorker worker;

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "fetch-financial-aid-basis-worker");
	}

	@Test
	void handleWithResponse() throws Exception {
		final Map<String, Map<String, Object>> response = Map.of(
			"af", Map.of("status", "approved", "amount", 7900),
			"csn", Map.of("loan", "active"));

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(response);

		final var output = worker.handle(task);

		verify(financialAidClientMock).getFinancialAidBasis("2281", "199001011234", "2026-01-01", "2026-05-20");
		assertThat(output).containsOnlyKeys("financialAidBasis");
		final var roundTrip = objectMapper.readValue(
			(String) output.get("financialAidBasis"),
			new TypeReference<LinkedHashMap<String, LinkedHashMap<String, Object>>>() {});
		assertThat(roundTrip).isEqualTo(response);
	}

	@Test
	void handleWithEmptyResponse() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(Map.of());

		assertThat(worker.handle(task)).isEqualTo(Map.of("financialAidBasis", "{}"));
	}

	@Test
	void handleWithNullResponse() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(null);

		assertThat(worker.handle(task)).isEqualTo(Map.of("financialAidBasis", "{}"));
	}

	@Test
	void handleWithCustomOutputVariable() throws Exception {
		final Map<String, Map<String, Object>> response = Map.of("fk", Map.of("bostadsbidrag", 1850));

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "198202022345")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20")
			.putValue("outputVariable", "coApplicantFinancialAidBasis"));
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(response);

		final var output = worker.handle(task);

		verify(financialAidClientMock).getFinancialAidBasis("2281", "198202022345", "2026-01-01", "2026-05-20");
		assertThat(output).containsOnlyKeys("coApplicantFinancialAidBasis");
		final var roundTrip = objectMapper.readValue(
			(String) output.get("coApplicantFinancialAidBasis"),
			new TypeReference<LinkedHashMap<String, LinkedHashMap<String, Object>>>() {});
		assertThat(roundTrip).isEqualTo(response);
	}

	@Test
	void handleWithBlankPersonalNumberSkipsFetch() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "")
			.putValue("fromDate", "2026-01-01")
			.putValue("toDate", "2026-05-20")
			.putValue("outputVariable", "coApplicantFinancialAidBasis"));

		final var output = worker.handle(task);

		verifyNoInteractions(financialAidClientMock);
		assertThat(output).isEqualTo(Map.of("coApplicantFinancialAidBasis", "{}"));
	}
}
