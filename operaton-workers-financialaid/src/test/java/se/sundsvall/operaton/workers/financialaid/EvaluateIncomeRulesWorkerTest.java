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
import se.sundsvall.operaton.workers.financialaid.rules.AgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.ChangeWarning;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedAgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedIncome;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesResult;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole.APPLICANT;

@ExtendWith(MockitoExtension.class)
class EvaluateIncomeRulesWorkerTest {

	private static final String BASIS_JSON = "{\"fk\":{\"utbetalningar\":[{\"nettobelopp\":{\"summa\":\"1850\"},\"datum\":\"2026-05-15\",\"formansfamilj\":{\"beskrivning\":\"Bostadsbidrag\"}}]}}";

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private IncomeRulesEvaluator evaluatorMock;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	private EvaluateIncomeRulesWorker worker;

	@BeforeEach
	void setUp() {
		worker = new EvaluateIncomeRulesWorker(externalTaskServiceMock, evaluatorMock, objectMapper);
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
		final var change = new ChangeWarning("Bostadsbidrag", new BigDecimal("-23"), new BigDecimal("2400"), new BigDecimal("1850"),
			"Bostadsbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(classified), List.of(change)));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isTrue();
		assertThat((String) output.get("incomeUnhandled")).isEmpty();
		assertThat((String) output.get("incomeChangeWarnings"))
			.isEqualTo("Bostadsbidrag: -23% – Bostadsbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");
		assertThat((String) output.get("classifiedIncomes")).contains("\"normberakning\":\"Bostadsbidrag\"").contains("\"atgard\":\"TA_MED_KVITTNING\"");
	}

	@Test
	void handleSurfacesAnUnverifiableAnswerAsSomethingToHandle() {
		// No incomes and no change warnings: without the answer, this errand would look perfectly clean while an
		// a-kassa had in fact not answered at all.
		final var unverifiable = new ClassifiedAgencyAnswer(
			new AgencyAnswer("so", "Unionens a-kassa", "9", false, false),
			"EJ_KONTROLLERBAR", "Statuskodens innebörd är inte fastställd");

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(), List.of(), List.of(unverifiable)));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isTrue();
		assertThat((String) output.get("incomeUnhandled"))
			.isEqualTo("A-kassa Unionens a-kassa: kunde inte kontrolleras – Statuskodens innebörd är inte fastställd");
		assertThat((String) output.get("incomeChangeWarnings")).isEmpty();
	}

	@Test
	void handleDoesNotFlagAnAnswerThatWasUsable() {
		final var answered = new ClassifiedAgencyAnswer(
			new AgencyAnswer("so", "Unionens a-kassa", "1", false, true), "SVARAT", "Organisationen har lämnat utbetalningar");

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(), List.of(), List.of(answered)));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isFalse();
		assertThat((String) output.get("incomeUnhandled")).isEmpty();
	}

	@Test
	void handleNamesTheOrganisationEvenWhenTheAnswerCarriesNone() {
		final var anonymous = new ClassifiedAgencyAnswer(
			new AgencyAnswer("so", null, "9", false, false), "EJ_KONTROLLERBAR", null);

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(), List.of(), List.of(anonymous)));

		assertThat((String) worker.handle(task).get("incomeUnhandled"))
			.isEqualTo("A-kassa (okänd organisation): kunde inte kontrolleras");
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
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(offList), List.of()));

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
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(), List.of()));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isFalse();
		assertThat((String) output.get("classifiedIncomes")).isEqualTo("[]");
	}

	@Test
	void handleRendersTheTwoSumsWhenThereIsNoPercentToShow() {
		// the exact comparison warns on a benefit that only exists this month, and a percentage of zero says nothing
		final var change = new ChangeWarning("Bostadsbidrag", null, BigDecimal.ZERO, new BigDecimal("4500"),
			"Bostadsbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(), List.of(change)));

		final var output = worker.handle(task);

		assertThat((Boolean) output.get("incomeHasWarnings")).isTrue();
		assertThat((String) output.get("incomeChangeWarnings"))
			.isEqualTo("Bostadsbidrag: 0 kr → 4500 kr – Bostadsbidrag föregående månad är inte samma summa som denna månad – kontrollera summan");
	}

	@Test
	void handleRendersPlainPercentWhenTheDmnCarriesNoRuleText() {
		// the threshold table published before 2026-09-17 has no regel output
		final var first = new ChangeWarning("Bostadsbidrag", new BigDecimal("-23"), new BigDecimal("2400"), new BigDecimal("1850"), null);
		final var second = new ChangeWarning("Dagersättning", new BigDecimal("15"), new BigDecimal("4000"), new BigDecimal("4600"), "   ");

		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("applicationMonth", "2026-06")
			.putValue("financialAidBasis", BASIS_JSON));
		when(evaluatorMock.evaluate(anyList(), anyList(), eq(YearMonth.of(2026, Month.JUNE))))
			.thenReturn(new IncomeRulesResult(List.of(), List.of(first, second)));

		final var output = worker.handle(task);

		assertThat((String) output.get("incomeChangeWarnings")).isEqualTo("Bostadsbidrag: -23%; Dagersättning: 15%");
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
