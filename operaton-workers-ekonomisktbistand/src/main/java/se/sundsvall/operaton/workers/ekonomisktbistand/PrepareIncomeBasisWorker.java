package se.sundsvall.operaton.workers.ekonomisktbistand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.RpaContext;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.caremanagement.CareManagementClient;
import se.sundsvall.operaton.workers.financialaid.FinancialAidClient;
import se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole;
import se.sundsvall.operaton.workers.financialaid.rules.ChangeWarning;
import se.sundsvall.operaton.workers.financialaid.rules.ClassifiedAgencyAnswer;
import se.sundsvall.operaton.workers.financialaid.rules.IncomeRulesEvaluator;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekAvailability;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncomeExtractor;
import se.sundsvall.operaton.workers.financialaid.rules.StudiehjalpSeptemberRule;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static org.springframework.util.StringUtils.hasText;

/**
 * The EB beredning, start to finish, in one external task: resolve the household, read SSBTEK, evaluate the income
 * rules and hand the result to caremanagement.
 *
 * <p>
 * It replaces five tasks — {@code fetch_ssbtek}, {@code fetch_coapplicant_ssbtek},
 * {@code evaluate_income_regelverk}, {@code evaluate_studiehjalp_september} and {@code prepare_normberakning} — which
 * existed as separate steps only so that each could hand the next its result. <strong>That handover was the
 * defect.</strong> A process variable lives in {@code ACT_RU_VARIABLE.TEXT_} and {@code ACT_HI_DETAIL.TEXT_}, both
 * {@code varchar(4000)}; the SSBTEK answer passed 4000 characters on 2026-09-22 and took the whole process down, and
 * the engine's history keeps whatever fits for the model's TTL, where no gallring reaches it.
 *
 * <p>
 * So nothing travels between the steps any more. The agency payload, the classified incomes and the warnings live in
 * local variables for the length of one task and leave as an HTTP request body, which has no column behind it. The
 * personal numbers are fetched per run from {@code /rpa-context} rather than seeded at process start — the same trade
 * the RPA queue items were built on, for the same reason, and every disclosure lands in the errand's event log.
 *
 * <p>
 * The only variable it writes is {@code ssbtekError}, a boolean the daily timer branches on.
 */
@Component
@TopicWorker(
	topic = "prepare-income-basis",
	description = "Prepares the EB income basis in one step: resolves the household's personal numbers from careM, reads SSBTEK for applicant and co-applicant, evaluates the income rules (allow list, thresholds, period rules and the seasonal studiehjälp rule via the published DMNs) and posts the classified incomes and warnings to careM's normberäkning prepare. Nothing but the ssbtekError flag becomes a process variable — the agency payload never leaves the worker, because the engine cannot store it and should not keep it.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		PrepareIncomeBasisWorker.VAR_NAMESPACE,
		PrepareIncomeBasisWorker.VAR_ERRAND_ID,
		PrepareIncomeBasisWorker.VAR_APPLICANT,
		PrepareIncomeBasisWorker.VAR_CO_APPLICANT,
		PrepareIncomeBasisWorker.VAR_APPLICATION_MONTH,
		PrepareIncomeBasisWorker.VAR_FROM_DATE,
		PrepareIncomeBasisWorker.VAR_TO_DATE
	},
	outputVariables = {
		PrepareIncomeBasisWorker.VAR_OUT_SSBTEK_ERROR
	})
public class PrepareIncomeBasisWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_FROM_DATE = "fromDate";
	static final String VAR_TO_DATE = "toDate";

	static final String VAR_OUT_SSBTEK_ERROR = "ssbtekError";

	/** caremanagement reads a blank basis plus {@code ssbtekError} as "leave the calculation as it stands". */
	private static final String NO_CLASSIFIED_INCOMES = "";
	private static final String OFF_LIST_ACTION = "EJ_PA_LISTAN";
	private static final String NO_APPLICANT_IDENTITY = "No personal number could be resolved for the applicant on errand %s";

	private static final Logger LOG = LoggerFactory.getLogger(PrepareIncomeBasisWorker.class);

	private final CareManagementClient careManagementClient;
	private final FinancialAidClient financialAidClient;
	private final IncomeRulesEvaluator evaluator;
	private final ObjectMapper objectMapper;

	public PrepareIncomeBasisWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient,
		final FinancialAidClient financialAidClient, final IncomeRulesEvaluator evaluator, final ObjectMapper objectMapper) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
		this.financialAidClient = financialAidClient;
		this.evaluator = evaluator;
		this.objectMapper = objectMapper;
	}

	@Dept44Scheduled(cron = "${scheduler.prepare-income-basis.cron:*/5 * * * * *}", name = "prepare-income-basis-worker", lockAtMostFor = "PT60S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var namespace = requireVariable(task, VAR_NAMESPACE, String.class);
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		final var applicationMonth = YearMonth.parse(requireVariable(task, VAR_APPLICATION_MONTH, String.class));
		final var fromDate = requireVariable(task, VAR_FROM_DATE, String.class);
		final var toDate = requireVariable(task, VAR_TO_DATE, String.class);

		final var household = household(municipalityId, namespace, errandId);

		final Map<String, Map<String, Object>> applicantBasis;
		final Map<String, Map<String, Object>> coApplicantBasis;
		try {
			applicantBasis = basis(municipalityId, household.getApplicantPersonId(), fromDate, toDate);
			coApplicantBasis = basis(municipalityId, household.getCoApplicantPersonId(), fromDate, toDate);
		} catch (final RuntimeException e) {
			// Ride out a downstream blip on the normal ladder first; only a failure that survives it is reported as a
			// read failure, which is what stops the rules running over data we know we could not read.
			if (!isFinalAttempt(task)) {
				throw e;
			}
			LOG.warn("SSBTEK could not be read and no retries remain - preparing the normberäkning as a read failure", e);
			return reportReadFailure(municipalityId, namespace, errandId, applicationMonth, task);
		}

		if (hasReadFailure(applicantBasis) || hasReadFailure(coApplicantBasis)) {
			LOG.warn("An income-bearing agency could not answer - skipping the income rules this run");
			return reportReadFailure(municipalityId, namespace, errandId, applicationMonth, task);
		}

		return prepare(municipalityId, namespace, errandId, applicationMonth, task, applicantBasis, coApplicantBasis, household);
	}

	/**
	 * The household's personal numbers, read per run rather than carried in the process. An applicant we cannot name
	 * cannot be looked up in SSBTEK, and that is a fault on the errand rather than a downstream outage — so it fails
	 * rather than degrading into a read-failure warning, which would tell the handläggare that SSBTEK was unavailable
	 * when it was never asked.
	 */
	private RpaContext household(final String municipalityId, final String namespace, final String errandId) {
		final var context = ofNullable(careManagementClient.getRpaContext(municipalityId, namespace, errandId).getBody())
			.orElseGet(RpaContext::new);
		if (!hasText(context.getApplicantPersonId())) {
			throw new IllegalStateException(NO_APPLICANT_IDENTITY.formatted(errandId));
		}
		return context;
	}

	/** An absent household member is no basis at all, not an empty read — the rules must not count them as answered. */
	private Map<String, Map<String, Object>> basis(final String municipalityId, final String personalNumber, final String fromDate, final String toDate) {
		if (!hasText(personalNumber)) {
			return Map.of();
		}
		return ofNullable(financialAidClient.getFinancialAidBasis(municipalityId, personalNumber, fromDate, toDate)).orElseGet(Map::of);
	}

	private Map<String, Object> prepare(final String municipalityId, final String namespace, final String errandId, final YearMonth applicationMonth,
		final LockedExternalTask task, final Map<String, Map<String, Object>> applicantBasis,
		final Map<String, Map<String, Object>> coApplicantBasis, final RpaContext household) {

		final var incomes = new ArrayList<>(SsbtekIncomeExtractor.extract(applicantBasis, ApplicantRole.APPLICANT));
		final var answers = new ArrayList<>(SsbtekIncomeExtractor.extractAnswers(applicantBasis));
		if (!coApplicantBasis.isEmpty()) {
			incomes.addAll(SsbtekIncomeExtractor.extract(coApplicantBasis, ApplicantRole.CO_APPLICANT));
			answers.addAll(SsbtekIncomeExtractor.extractAnswers(coApplicantBasis));
		}

		final var result = evaluator.evaluate(incomes, answers, applicationMonth);

		// An organisation whose answer could not be verified is an unhandled item for the handläggare, not an absence
		// of income: SSBTEK answering "I cannot say" must never read as "this person has nothing".
		final var unverifiable = result.answers().stream()
			.filter(ClassifiedAgencyAnswer::unverifiable)
			.map(PrepareIncomeBasisWorker::renderAnswer)
			.distinct()
			.toList();
		final var unhandled = concat(
			result.classified().stream()
				.filter(classified -> classified.warning() || OFF_LIST_ACTION.equals(classified.action()))
				.map(classified -> classified.income().benefit() + " (" + classified.action() + ")"),
			unverifiable.stream())
			.distinct()
			.toList();

		final var changeWarnings = new ArrayList<>(result.changeWarnings().stream()
			.map(PrepareIncomeBasisWorker::render)
			.toList());
		studiehjalpWarning(municipalityId, household.getApplicantPersonId(), applicationMonth).ifPresent(changeWarnings::add);

		final var request = request(errandId, applicationMonth, task)
			.classifiedIncomes(serialize(result.classified()))
			.unhandledIncomes(unhandled)
			.changeWarnings(List.copyOf(changeWarnings))
			.ssbtekError(false);
		careManagementClient.prepareNormberakning(municipalityId, namespace, request);

		LOG.info("Income basis prepared ({} transferable incomes, {} unhandled, {} change warnings)",
			result.classified().size(), unhandled.size(), changeWarnings.size());
		return Map.of(VAR_OUT_SSBTEK_ERROR, false);
	}

	/**
	 * Tell caremanagement the basis could not be read. The classified incomes are left <strong>blank</strong> rather
	 * than an empty list: an empty list reads as "this month has no incomes" and would clear the rows the previous run
	 * transferred.
	 */
	private Map<String, Object> reportReadFailure(final String municipalityId, final String namespace, final String errandId,
		final YearMonth applicationMonth, final LockedExternalTask task) {

		careManagementClient.prepareNormberakning(municipalityId, namespace, request(errandId, applicationMonth, task)
			.classifiedIncomes(NO_CLASSIFIED_INCOMES)
			.unhandledIncomes(List.of())
			.changeWarnings(List.of())
			.ssbtekError(true));

		return Map.of(VAR_OUT_SSBTEK_ERROR, true);
	}

	private NormberakningRequest request(final String errandId, final YearMonth applicationMonth, final LockedExternalTask task) {
		final var request = new NormberakningRequest()
			.errandId(errandId)
			.applicationMonth(applicationMonth.toString())
			.applicant(requireVariable(task, VAR_APPLICANT, String.class));
		optionalVariable(task, VAR_CO_APPLICANT, String.class).filter(StringUtils::hasText).ifPresent(request::coApplicant);
		return request;
	}

	/**
	 * Verksamhetens seasonal studiehjälp rule, which needs a wider SSBTEK window than the beredning's own and reads
	 * nothing in the other eleven months. It used to append to a process variable that the next task read back, which
	 * only worked while the task ordering held; here it is simply another entry in the same list.
	 *
	 * <p>
	 * A failure is swallowed rather than retried: the rule is an extra check on top of a normberäkning that is
	 * otherwise complete, and failing the task would roll all of it back over a seasonal question.
	 */
	private Optional<String> studiehjalpWarning(final String municipalityId, final String personalNumber, final YearMonth applicationMonth) {
		if (!StudiehjalpSeptemberRule.appliesTo(applicationMonth)) {
			return Optional.empty();
		}
		try {
			final var from = StudiehjalpSeptemberRule.readFrom(applicationMonth).atDay(1);
			final var wider = basis(municipalityId, personalNumber, from.toString(), applicationMonth.atEndOfMonth().toString());
			final var warning = StudiehjalpSeptemberRule.evaluate(applicationMonth, SsbtekIncomeExtractor.extract(wider, ApplicantRole.APPLICANT));

			LOG.info("Studiehjälp september rule evaluated (warning: {})", warning.isPresent());
			return warning;
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the wider SSBTEK window for the studiehjälp september rule - skipping it this run", e);
			return Optional.empty();
		}
	}

	private boolean hasReadFailure(final Map<String, Map<String, Object>> basis) {
		if (basis.isEmpty()) {
			return false;
		}
		// Deliberately a tree rather than the map: the FEEL engine contributes a Jackson module, and an instanceof Map
		// test against its own map type silently misses every agency error.
		return SsbtekAvailability.hasReadFailure(objectMapper.valueToTree(basis));
	}

	/**
	 * The case worker's one-line rendering of a change warning: the change in percent, or the two sums when there is no
	 * comparison sum to take a percentage of, followed by verksamhetens warning text from the DMN when there is one.
	 */
	private static String render(final ChangeWarning warning) {
		final var change = warning.benefit() + ": " + changeText(warning);
		if (hasText(warning.rule())) {
			return change + " – " + warning.rule();
		}
		return change;
	}

	/**
	 * A verified-nothing and an unanswerable organisation look identical in the income list, so the text has to say
	 * which one this is - phrased as a statement about our check, never about the sökande.
	 */
	private static String renderAnswer(final ClassifiedAgencyAnswer classified) {
		final var organisation = classified.answer().organisation();
		if (!hasText(organisation)) {
			return "A-kassa (okänd organisation): kunde inte kontrolleras";
		}
		final var who = "A-kassa " + organisation;
		if (hasText(classified.rule())) {
			return who + ": kunde inte kontrolleras – " + classified.rule();
		}
		return who + ": kunde inte kontrolleras";
	}

	private static String changeText(final ChangeWarning warning) {
		if (warning.changePercent() == null) {
			return warning.comparisonSum() + " kr → " + warning.controlSum() + " kr";
		}
		return warning.changePercent() + "%";
	}

	private String serialize(final List<?> classified) {
		try {
			return objectMapper.writeValueAsString(classified);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize the classified incomes", e);
		}
	}

}
