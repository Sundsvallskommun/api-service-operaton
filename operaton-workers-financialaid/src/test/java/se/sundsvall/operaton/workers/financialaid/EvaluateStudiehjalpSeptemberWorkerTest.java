package se.sundsvall.operaton.workers.financialaid;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
class EvaluateStudiehjalpSeptemberWorkerTest {

	private static final String WARNING = "Studiehjälp utbetalades i juni men utbetalning saknas i september – kontrollera om ungdom ska ha studiehjälp";

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FinancialAidClient financialAidClientMock;

	@InjectMocks
	private EvaluateStudiehjalpSeptemberWorker worker;

	/** A CSN answer with one studiehjälp payment on the given date, in the shape SsbtekIncomeExtractor reads. */
	private static Map<String, Map<String, Object>> csnPayments(final String... isoDates) {
		final var arenden = List.of(Map.of(
			"klartext", "Studiebidrag",
			"UtbetalningsPlan", Map.of("Utbetalning", List.of(isoDates).stream()
				.map(date -> (Object) Map.of("totbelopp", "1250", "utbetdatum", date.replace("-", "")))
				.toList())));
		return Map.of("csn", Map.of("Personer", Map.of("Person", List.of(Map.of("Studiehjalp", Map.of("Arenden", Map.of("Arende", arenden)))))));
	}

	private static LockedExternalTask task(final String applicationMonth, final String existingWarnings) {
		final var task = mock(LockedExternalTask.class);
		final var variables = Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("personalNumber", "199001011234")
			.putValue("applicationMonth", applicationMonth);
		if (existingWarnings != null) {
			variables.putValue("incomeChangeWarnings", existingWarnings);
		}
		when(task.getVariables()).thenReturn(variables);
		return task;
	}

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "evaluate-studiehjalp-september-worker");
	}

	@Test
	void readsNothingOutsideOctober() {
		// The seasonal rule cannot fire, so the extra disclosure of a person's agency data must not happen either.
		final var output = worker.handle(task("2026-09", "Bostadsbidrag ändrat"));

		assertThat(output).containsExactly(Map.entry("incomeChangeWarnings", "Bostadsbidrag ändrat"));
		verifyNoInteractions(financialAidClientMock);
	}

	@Test
	void warnsWhenJunePaidAndSeptemberDidNot() {
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(csnPayments("2026-06-30"));

		final var output = worker.handle(task("2026-10", null));

		assertThat(output).containsExactly(Map.entry("incomeChangeWarnings", WARNING));
		// June through the end of the application month — the window the ordinary fetch does not reach.
		verify(financialAidClientMock).getFinancialAidBasis("2281", "199001011234", "2026-06-01", "2026-10-31");
	}

	@Test
	void silentWhenSeptemberPaidToo() {
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(csnPayments("2026-06-30", "2026-09-29"));

		assertThat(worker.handle(task("2026-10", ""))).containsExactly(Map.entry("incomeChangeWarnings", ""));
	}

	@Test
	void appendsToTheExistingWarningsRatherThanReplacingThem() {
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(csnPayments("2026-06-30"));

		final var output = worker.handle(task("2026-10", "Bostadsbidrag ändrat"));

		assertThat(output).containsExactly(Map.entry("incomeChangeWarnings", "Bostadsbidrag ändrat; " + WARNING));
	}

	@Test
	void keepsTheWarningsWhenTheWiderReadFails() {
		// The normberäkning is already prepared; failing the task would roll all of it back over a seasonal check.
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenThrow(new IllegalStateException("financial-aid down"));

		assertThat(worker.handle(task("2026-10", "Bostadsbidrag ändrat")))
			.containsExactly(Map.entry("incomeChangeWarnings", "Bostadsbidrag ändrat"));
	}

	@Test
	void skipsWithoutAPersonalNumber() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("applicationMonth", "2026-10")
			.putValue("incomeChangeWarnings", "Bostadsbidrag ändrat"));

		assertThat(worker.handle(task)).containsExactly(Map.entry("incomeChangeWarnings", "Bostadsbidrag ändrat"));
		verifyNoInteractions(financialAidClientMock);
	}

	@Test
	void handlesAnEmptyBasis() {
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(Map.of());

		assertThat(worker.handle(task("2026-10", null))).containsExactly(Map.entry("incomeChangeWarnings", ""));
	}
}
