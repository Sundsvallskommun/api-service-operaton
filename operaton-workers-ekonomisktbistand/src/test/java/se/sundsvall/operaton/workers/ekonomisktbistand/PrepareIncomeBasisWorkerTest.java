package se.sundsvall.operaton.workers.ekonomisktbistand;

import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.caremanagement.HouseholdIdentifiers;
import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;
import org.springframework.http.ResponseEntity;
import se.sundsvall.operaton.workers.caremanagement.CareManagementClient;
import se.sundsvall.operaton.workers.financialaid.FinancialAidClient;
import se.sundsvall.operaton.workers.financialaid.rules.AgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.ChangeWarning;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedAgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedIncome;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesResult;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareIncomeBasisWorkerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "3ddab9bd-2913-479c-bcba-2bdedc372d0b";
	private static final String APPLICANT_PARTY_ID = "cbfdcea3-72d9-40ee-ad9c-4472b6b37bd1";
	private static final String APPLICANT_PNR = "199001011234";
	private static final String CO_APPLICANT_PNR = "198505050505";

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CareManagementClient careManagementClientMock;

	@Mock
	private FinancialAidClient financialAidClientMock;

	@Mock
	private IncomeRulesEvaluator evaluatorMock;

	@Mock
	private LockedExternalTask taskMock;

	private PrepareIncomeBasisWorker worker() {
		return new PrepareIncomeBasisWorker(externalTaskServiceMock, careManagementClientMock, financialAidClientMock, evaluatorMock, new ObjectMapper());
	}

	private void task(final Integer retries) {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getRetries()).thenReturn(retries);
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", MUNICIPALITY_ID)
			.putValue("namespace", NAMESPACE)
			.putValue("errandId", ERRAND_ID)
			.putValue("applicant", APPLICANT_PARTY_ID)
			.putValue("applicationMonth", "2026-06")
			.putValue("fromDate", "2026-04-01")
			.putValue("toDate", "2026-06-30"));
	}

	private void household(final String applicantPnr, final String coApplicantPnr) {
		lenient().when(careManagementClientMock.getHouseholdIdentifiers(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(ResponseEntity.ok(new HouseholdIdentifiers().errandNumber("EB-26060001").applicantPersonId(applicantPnr).coApplicantPersonId(coApplicantPnr)));
		lenient().when(careManagementClientMock.prepareNormberakning(any(), any(), any()))
			.thenReturn(ResponseEntity.ok(new NormberakningResponse()));
	}

	private static Map<String, Map<String, Object>> basis() {
		return Map.of("fk", Map.of("utlamnare", List.of()));
	}

	private NormberakningRequest captureRequest() {
		final var captor = ArgumentCaptor.forClass(NormberakningRequest.class);
		verify(careManagementClientMock).prepareNormberakning(eq(MUNICIPALITY_ID), eq(NAMESPACE), captor.capture());
		return captor.getValue();
	}

	/**
	 * The whole point of the merge: the agency payload, the classified incomes and the warnings leave as an HTTP body,
	 * and the only thing that becomes a process variable is a boolean. A regression here puts a person's income data
	 * back into a varchar(4000) the engine keeps for the model's history TTL.
	 */
	@Test
	void writesNothingButTheSsbtekErrorFlagToTheProcess() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of(), List.of()));

		final var output = worker().handle(taskMock);

		assertThat(output).containsExactly(org.assertj.core.api.Assertions.entry("ssbtekError", false));
	}

	@Test
	void resolvesTheHouseholdFromCareManagementAndReadsBothMembers() {
		task(null);
		household(APPLICANT_PNR, CO_APPLICANT_PNR);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, CO_APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(
			List.of(new ClassifiedIncome(new SsbtekIncome("Bostadsbidrag", null, null, BigDecimal.valueOf(4500), null, null, null, null, null), "TA_MED", "Bostadsbidrag", false, "Ta med")),
			List.of(new ChangeWarning("Bostadsbidrag", BigDecimal.valueOf(23), null, null, "Kontrollera summan"))));

		worker().handle(taskMock);

		final var request = captureRequest();
		assertThat(request.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
		assertThat(request.getSsbtekError()).isFalse();
		assertThat(request.getClassifiedIncomes()).contains("Bostadsbidrag");
		assertThat(request.getChangeWarnings()).containsExactly("Bostadsbidrag: 23% – Kontrollera summan");
	}

	/** No co-applicant means no second disclosure of anyone's agency data. */
	@Test
	void readsOnlyTheApplicantWhenThereIsNoCoApplicant() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of()));

		worker().handle(taskMock);

		verify(financialAidClientMock).getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30");
		verify(financialAidClientMock, never()).getFinancialAidBasis(eq(MUNICIPALITY_ID), eq(CO_APPLICANT_PNR), any(), any());
	}

	/**
	 * The applicant's AF decision and FK days travel to careM's day check; an unread AF stays unread, not "no decision".
	 */
	@Test
	void sendsTheApplicantsDayCheckBasis() {
		task(null);
		household(APPLICANT_PNR, CO_APPLICANT_PNR);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(Map.of(
			"fk", Map.of("formansinformation", Map.of("programjobdagar", List.of(Map.of("antalForbrukade", 212, "harForbrukatMaxAntal", false))))));
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, CO_APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(Map.of(
			"af", Map.of("Svar", Map.of("BeslutInfo", Map.of("EkonomiskaBeslut", Map.of("Beslut", Map.of("BeslutFrom", "2026-05-01")))))));
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of()));

		worker().handle(taskMock);

		final var basis = captureRequest().getDayCheckBasis();
		assertThat(basis.getEconomicDecisionPeriods()).isNull();
		assertThat(basis.getConsumedDays()).isEqualTo(212);
		assertThat(basis.getAllDaysConsumed()).isFalse();
	}

	@Test
	void sendsTheApplicantsEconomicDecisionPeriods() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(Map.of(
			"af", Map.of("Svar", Map.of("BeslutInfo", Map.of("EkonomiskaBeslut", Map.of("Beslut", Map.of("BeslutFrom", "2026-05-01", "BeslutTom", "2026-06-30")))))));
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of()));

		worker().handle(taskMock);

		final var basis = captureRequest().getDayCheckBasis();
		assertThat(basis.getEconomicDecisionPeriods()).singleElement().satisfies(period -> {
			assertThat(period.getFromDate()).isEqualTo(LocalDate.of(2026, 5, 1));
			assertThat(period.getToDate()).isEqualTo(LocalDate.of(2026, 6, 30));
		});
		assertThat(basis.getConsumedDays()).isNull();
		assertThat(basis.getAllDaysConsumed()).isNull();
	}

	/**
	 * An agency that could not answer must stop the rules: running them over a basis we know we could not read reports
	 * an income we never saw as an income the sökande does not have.
	 */
	@Test
	void skipsTheRulesAndReportsAReadFailureWhenAnAgencyCouldNotAnswer() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30"))
			.thenReturn(Map.of("fk", Map.of("error", Map.of("kalla", "financial-aid"))));

		final var output = worker().handle(taskMock);

		assertThat(output).containsExactly(org.assertj.core.api.Assertions.entry("ssbtekError", true));
		final var request = captureRequest();
		assertThat(request.getSsbtekError()).isTrue();
		// Blank, not an empty list: caremanagement reads an empty list as "this month has no incomes" and would clear
		// the rows the previous run transferred.
		assertThat(request.getClassifiedIncomes()).isEmpty();
		assertThat(request.getChangeWarnings()).isEmpty();
		verifyNoInteractions(evaluatorMock);
	}

	/** A downstream blip rides the retry ladder rather than degrading a recoverable outage into a warning. */
	@Test
	void rethrowsWhenTheReadFailsAndRetriesRemain() {
		task(3);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenThrow(new IllegalStateException("gateway down"));

		assertThatThrownBy(() -> worker().handle(taskMock)).isInstanceOf(IllegalStateException.class).hasMessage("gateway down");

		verify(careManagementClientMock, never()).prepareNormberakning(any(), any(), any());
	}

	@Test
	void reportsAReadFailureWhenTheReadFailsAndNoRetriesRemain() {
		task(1);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(any(), any(), any(), any())).thenThrow(new IllegalStateException("gateway down"));

		assertThat(worker().handle(taskMock)).containsExactly(org.assertj.core.api.Assertions.entry("ssbtekError", true));
		assertThat(captureRequest().getSsbtekError()).isTrue();
	}

	/**
	 * An applicant we cannot name was never asked, so this is not a read failure. Degrading it into one would tell the
	 * handläggare SSBTEK was unavailable when the fault is on the errand.
	 */
	@Test
	void failsRatherThanDegradingWhenTheApplicantHasNoPersonalNumber() {
		task(null);
		household(null, null);

		assertThatThrownBy(() -> worker().handle(taskMock))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No personal number could be resolved for the applicant")
			.hasMessageContaining(ERRAND_ID);

		verifyNoInteractions(financialAidClientMock, evaluatorMock);
	}

	private void taskForMonth(final String month, final String from, final String to) {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getRetries()).thenReturn(null);
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", MUNICIPALITY_ID)
			.putValue("namespace", NAMESPACE)
			.putValue("errandId", ERRAND_ID)
			.putValue("applicant", APPLICANT_PARTY_ID)
			.putValue("applicationMonth", month)
			.putValue("fromDate", from)
			.putValue("toDate", to));
	}

	/**
	 * The seasonal rule needs a wider SSBTEK window than the beredning's own, and it used to live in its own task that
	 * appended to a process variable the next task read back. Here it is one more read and one more entry in the list.
	 */
	@Test
	void readsTheWiderWindowForTheSeasonalStudiehjalpRuleInOctober() {
		taskForMonth("2026-10", "2026-08-01", "2026-10-31");
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-08-01", "2026-10-31")).thenReturn(basis());
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-06-01", "2026-10-31")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of()));

		worker().handle(taskMock);

		verify(financialAidClientMock).getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-06-01", "2026-10-31");
	}

	/** Eleven months a year the rule cannot fire, and an extra disclosure to ask it anyway is not worth paying. */
	@Test
	void doesNotReadTheWiderWindowOutsideOctober() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of()));

		worker().handle(taskMock);

		verify(financialAidClientMock).getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30");
		verify(financialAidClientMock, never()).getFinancialAidBasis(eq(MUNICIPALITY_ID), eq(APPLICANT_PNR), eq("2026-06-01"), any());
	}

	/**
	 * The seasonal read failing must not roll back a normberäkning that is otherwise complete — the handläggare loses
	 * one warning, which the next daily run raises.
	 */
	@Test
	void preparesAnywayWhenTheSeasonalReadFails() {
		taskForMonth("2026-10", "2026-08-01", "2026-10-31");
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-08-01", "2026-10-31")).thenReturn(basis());
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-06-01", "2026-10-31")).thenThrow(new IllegalStateException("wider read down"));
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of()));

		assertThat(worker().handle(taskMock)).containsExactly(org.assertj.core.api.Assertions.entry("ssbtekError", false));
		assertThat(captureRequest().getSsbtekError()).isFalse();
	}

	/**
	 * An a-kassa that could not answer is an unhandled item, phrased as a statement about our check and never about the
	 * sökande — and it must not read as "this person has nothing".
	 */
	@Test
	void surfacesAnUnverifiableAgencyAnswerAsUnhandled() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of(), List.of(
			new ClassifiedAgencyAnswer(new AgencyAnswer("so", "Unionens a-kassa", "okänd", false, false), "EJ_KONTROLLERBAR", "Kunde inte tolkas"),
			new ClassifiedAgencyAnswer(new AgencyAnswer("so", null, "okänd", false, false), "EJ_KONTROLLERBAR", null),
			new ClassifiedAgencyAnswer(new AgencyAnswer("so", "Svarande", "ok", true, true), "SVARAT", null))));

		worker().handle(taskMock);

		assertThat(captureRequest().getUnhandledIncomes()).containsExactlyInAnyOrder(
			"A-kassa Unionens a-kassa: kunde inte kontrolleras – Kunde inte tolkas",
			"A-kassa (okänd organisation): kunde inte kontrolleras");
	}

	/** With no comparison sum there is no percentage to take, so the warning shows the two sums instead. */
	@Test
	void rendersTwoSumsWhenThereIsNoComparisonPercentage() {
		task(null);
		household(APPLICANT_PNR, null);
		when(financialAidClientMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT_PNR, "2026-04-01", "2026-06-30")).thenReturn(basis());
		when(evaluatorMock.evaluate(any(), any(), any())).thenReturn(new IncomeRulesResult(List.of(), List.of(
			new ChangeWarning("Bostadsbidrag", null, BigDecimal.ZERO, BigDecimal.valueOf(4500), null))));

		worker().handle(taskMock);

		assertThat(captureRequest().getChangeWarnings()).containsExactly("Bostadsbidrag: 0 kr → 4500 kr");
	}
}
