package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;
import se.sundsvall.operaton.workers.financialaid.regelverk.ChangeWarning;
import se.sundsvall.operaton.workers.financialaid.regelverk.ClassifiedIncome;
import se.sundsvall.operaton.workers.financialaid.regelverk.IncomeRegelverkEvaluator;
import se.sundsvall.operaton.workers.financialaid.regelverk.IncomeRegelverkResult;
import se.sundsvall.operaton.workers.financialaid.regelverk.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.operaton.workers.financialaid.regelverk.ApplicantRole.APPLICANT;

@ExtendWith(MockitoExtension.class)
class EvaluateIncomeRegelverkWorkerTest {

	private static final String BASIS_JSON = "{\"fk\":{\"utbetalningar\":[{\"nettobelopp\":{\"summa\":\"1850\"},\"datum\":\"2026-05-15\",\"formansfamilj\":{\"beskrivning\":\"Bostadsbidrag\"}}]}}";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private IncomeRegelverkEvaluator evaluatorMock;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	private EvaluateIncomeRegelverkWorker worker;

	@BeforeEach
	void setUp() {
		worker = new EvaluateIncomeRegelverkWorker(externalTaskServiceMock, evaluatorMock, objectMapper);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> runWith(final Map<String, Object> variables, final IncomeRegelverkResult result) {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		final var vars = Variables.createVariables();
		variables.forEach(vars::putValue);
		when(task.getVariables()).thenReturn(vars);
		when(evaluatorMock.evaluate(anyList(), eq(YearMonth.of(2026, 6)))).thenReturn(result);

		worker.execute();

		final var captor = ArgumentCaptor.forClass(Map.class);
		verify(externalTaskServiceMock).complete(eq("task-1"), eq("evaluate-income-regelverk-worker"), captor.capture());
		return captor.getValue();
	}

	@Test
	void evaluatesAndOutputsClassifiedPlusWarnings() {
		final var classified = new ClassifiedIncome(
			new SsbtekIncome("Bostadsbidrag", null, null, new BigDecimal("1850"), LocalDate.of(2026, 5, 15), APPLICANT),
			"TA_MED_KVITTNING", "Bostadsbidrag", false, "Ta med kvittning");
		final var change = new ChangeWarning("Bostadsbidrag", new BigDecimal("-23"), new BigDecimal("2400"), new BigDecimal("1850"));

		final var output = runWith(
			Map.of("applicationMonth", "2026-06", "financialAidBasis", BASIS_JSON),
			new IncomeRegelverkResult(List.of(classified), List.of(change)));

		assertThat(output.get("incomeHasWarnings")).isEqualTo(true);
		assertThat((String) output.get("incomeUnhandled")).isEmpty();
		assertThat((String) output.get("incomeChangeWarnings")).contains("Bostadsbidrag: -23%");
		assertThat((String) output.get("classifiedIncomes")).contains("\"normberakning\":\"Bostadsbidrag\"").contains("\"atgard\":\"TA_MED_KVITTNING\"");
	}

	@Test
	void flagsOffListAndNoChangeWarnings() {
		final var offList = new ClassifiedIncome(
			new SsbtekIncome("Något okänt", null, null, new BigDecimal("500"), LocalDate.of(2026, 5, 10), APPLICANT),
			"EJ_PA_LISTAN", "-", true, "Ej på rålistan");

		final var output = runWith(
			Map.of("applicationMonth", "2026-06", "financialAidBasis", BASIS_JSON, "coApplicantFinancialAidBasis", BASIS_JSON),
			new IncomeRegelverkResult(List.of(offList), List.of()));

		assertThat(output.get("incomeHasWarnings")).isEqualTo(true);
		assertThat((String) output.get("incomeUnhandled")).contains("Något okänt (EJ_PA_LISTAN)");
		assertThat((String) output.get("incomeChangeWarnings")).isEmpty();
	}

	@Test
	void noWarningsWhenAllHandledAndStable() {
		final var output = runWith(
			Map.of("applicationMonth", "2026-06", "financialAidBasis", BASIS_JSON),
			new IncomeRegelverkResult(List.of(), List.of()));

		assertThat(output.get("incomeHasWarnings")).isEqualTo(false);
		assertThat((String) output.get("classifiedIncomes")).isEqualTo("[]");
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "evaluate-income-regelverk-worker");
	}

	@Test
	void malformedBasisFailsTheTask() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-9");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", "{not-json"));

		worker.execute();

		verify(externalTaskServiceMock).handleFailure(eq("task-9"), eq("evaluate-income-regelverk-worker"), any(), eq(0), eq(0L));
	}
}
