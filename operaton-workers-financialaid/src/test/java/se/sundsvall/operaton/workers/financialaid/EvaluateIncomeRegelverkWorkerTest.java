package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;
import se.sundsvall.operaton.workers.financialaid.regelverk.ChangeWarning;
import se.sundsvall.operaton.workers.financialaid.regelverk.ClassifiedIncome;
import se.sundsvall.operaton.workers.financialaid.regelverk.IncomeRegelverkEvaluator;
import se.sundsvall.operaton.workers.financialaid.regelverk.IncomeRegelverkResult;
import se.sundsvall.operaton.workers.financialaid.regelverk.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.operaton.workers.financialaid.regelverk.ApplicantRole.APPLICANT;

@ExtendWith(MockitoExtension.class)
class EvaluateIncomeRegelverkWorkerTest {

	private static final String BASIS_JSON = "{\"fk\":{\"utbetalningar\":[{\"nettobelopp\":{\"summa\":\"1850\"},\"datum\":\"2026-05-15\",\"formansfamilj\":{\"beskrivning\":\"Bostadsbidrag\"}}]}}";

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private IncomeRegelverkEvaluator evaluatorMock;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	private EvaluateIncomeRegelverkWorker worker;

	@BeforeEach
	void setUp() {
		worker = new EvaluateIncomeRegelverkWorker(externalTaskServiceMock, evaluatorMock, objectMapper);
	}

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "evaluate-income-regelverk-worker");
	}

	@Test
	void handleEvaluatesAndOutputsClassifiedPlusWarnings() {
		final var classified = new ClassifiedIncome(
			new SsbtekIncome("Bostadsbidrag", null, null, new BigDecimal("1850"), LocalDate.of(2026, Month.MAY, 15), APPLICANT),
			"TA_MED_KVITTNING", "Bostadsbidrag", false, "Ta med kvittning");
		final var change = new ChangeWarning("Bostadsbidrag", new BigDecimal("-23"), new BigDecimal("2400"), new BigDecimal("1850"));

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRegelverkResult(List.of(classified), List.of(change)));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isTrue();
		assertThat((String) output.get("incomeUnhandled")).isEmpty();
		assertThat((String) output.get("incomeChangeWarnings")).contains("Bostadsbidrag: -23%");
		assertThat((String) output.get("classifiedIncomes")).contains("\"normberakning\":\"Bostadsbidrag\"").contains("\"atgard\":\"TA_MED_KVITTNING\"");
	}

	@Test
	void handleFlagsOffListAndNoChangeWarnings() {
		final var offList = new ClassifiedIncome(
			new SsbtekIncome("Något okänt", null, null, new BigDecimal("500"), LocalDate.of(2026, Month.MAY, 10), APPLICANT),
			"EJ_PA_LISTAN", "-", true, "Ej på rålistan");

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON)
			.putValue("coApplicantFinancialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRegelverkResult(List.of(offList), List.of()));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isTrue();
		assertThat((String) output.get("incomeUnhandled")).contains("Något okänt (EJ_PA_LISTAN)");
		assertThat((String) output.get("incomeChangeWarnings")).isEmpty();
	}

	@Test
	void handleNoWarningsWhenAllHandledAndStable() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRegelverkResult(List.of(), List.of()));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isFalse();
		assertThat((String) output.get("classifiedIncomes")).isEqualTo("[]");
	}

	@Test
	void handleThrowsWhenApplicationMonthMissing() {
		final var task = mock(LockedExternalTask.class);
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> worker.handle(task))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'applicationMonth' is missing on task task-1");
	}

	@Test
	void handleThrowsOnMalformedBasis() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", "{not-json"));

		assertThatThrownBy(() -> worker.handle(task))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to parse financial-aid basis JSON");
	}
}
