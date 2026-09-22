package se.sundsvall.operaton.workers.financialaid;

import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.financialaid.rules.ApplicantRole;
import se.sundsvall.operaton.workers.financialaid.rules.SsbtekIncomeExtractor;
import se.sundsvall.operaton.workers.financialaid.rules.StudiehjalpSeptemberRule;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.util.Optional.ofNullable;

/**
 * Evaluates verksamhetens säsongsregel for studiehjälp, which needs SSBTEK data from outside the normal window.
 *
 * <p>
 * The rule asks whether a gymnasieelev who had studiehjälp in June has it again in September; the ordinary fetch only
 * reaches the comparison and control periods (August and September for an October application), so June is four
 * months out of reach. Verksamheten was asked where the June figure should come from and answered on 2026-09-22:
 * <em>ställ frågan längre tillbaka i tiden endast för studiehjälps-frågan</em>, and explicitly <em>no</em> to storing
 * it ourselves.
 * </p>
 *
 * <p>
 * So this worker makes its own, separate SSBTEK read over June-to-application-month, uses it for nothing but this one
 * rule, and keeps the ordinary read untouched — widening that one would quietly feed four months of payments to rules
 * written for two.
 * </p>
 *
 * <p>
 * <strong>It reads nothing outside October.</strong> The rule is seasonal, and an extra disclosure of a person's
 * agency data eleven months a year to answer a question that cannot fire is not a cost worth paying.
 * </p>
 *
 * <p>
 * The warning joins {@code incomeChangeWarnings} rather than travelling in a variable of its own: that is the channel
 * caremanagement already turns into warnings on the errand, so the handläggare sees this one beside the others with
 * no change at the other end.
 * </p>
 */
@Component
@TopicWorker(
	topic = "evaluate-studiehjalp-september",
	description = "Evaluates verksamhetens seasonal studiehjälp rule for an October application: studiehjälp paid in June but missing in September raises a warning. Makes its own SSBTEK read from June onwards, since the ordinary fetch only covers the comparison and control periods, and reads nothing at all in the other eleven months. The warning is appended to incomeChangeWarnings, the channel caremanagement already turns into errand warnings.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		EvaluateStudiehjalpSeptemberWorker.VAR_PERSONAL_NUMBER,
		EvaluateStudiehjalpSeptemberWorker.VAR_APPLICATION_MONTH,
		EvaluateStudiehjalpSeptemberWorker.VAR_INCOME_CHANGE_WARNINGS
	},
	outputVariables = {
		EvaluateStudiehjalpSeptemberWorker.VAR_INCOME_CHANGE_WARNINGS
	})
public class EvaluateStudiehjalpSeptemberWorker extends AbstractTopicWorker {

	static final String VAR_PERSONAL_NUMBER = "personalNumber";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	static final String VAR_INCOME_CHANGE_WARNINGS = "incomeChangeWarnings";

	/** The separator caremanagement splits {@code incomeChangeWarnings} on. */
	private static final String WARNING_SEPARATOR = "; ";

	private static final Logger LOG = LoggerFactory.getLogger(EvaluateStudiehjalpSeptemberWorker.class);

	private final FinancialAidClient financialAidClient;

	public EvaluateStudiehjalpSeptemberWorker(final ExternalTaskService externalTaskService, final FinancialAidClient financialAidClient) {
		super(externalTaskService);
		this.financialAidClient = financialAidClient;
	}

	@Dept44Scheduled(cron = "${scheduler.evaluate-studiehjalp-september.cron:*/5 * * * * *}", name = "evaluate-studiehjalp-september-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var existingWarnings = optionalVariable(task, VAR_INCOME_CHANGE_WARNINGS, String.class).orElse("");
		final var applicationMonth = YearMonth.parse(requireVariable(task, VAR_APPLICATION_MONTH, String.class));

		if (!StudiehjalpSeptemberRule.appliesTo(applicationMonth)) {
			return Map.of(VAR_INCOME_CHANGE_WARNINGS, existingWarnings);
		}

		final var personalNumber = optionalVariable(task, VAR_PERSONAL_NUMBER, String.class)
			.filter(StringUtils::hasText)
			.orElse(null);
		if (personalNumber == null) {
			LOG.info("No personal number supplied - skipping the studiehjälp september rule");
			return Map.of(VAR_INCOME_CHANGE_WARNINGS, existingWarnings);
		}

		return Map.of(VAR_INCOME_CHANGE_WARNINGS, append(existingWarnings, warningFor(task, applicationMonth, personalNumber)));
	}

	/**
	 * The rule's verdict, or nothing when the wider read fails.
	 * <p>
	 * A failure here is deliberately swallowed rather than retried: the rule is an extra check on top of a
	 * normberäkning that has already been prepared, and failing the task would roll the whole prepare back over a
	 * seasonal question. The handläggare loses one warning, which the next daily run raises.
	 */
	private Optional<String> warningFor(final LockedExternalTask task, final YearMonth applicationMonth, final String personalNumber) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var from = StudiehjalpSeptemberRule.readFrom(applicationMonth).atDay(1);
		final var to = applicationMonth.atEndOfMonth();

		try {
			final var basis = financialAidClient.getFinancialAidBasis(municipalityId, personalNumber, from.toString(), to.toString());
			final var incomes = SsbtekIncomeExtractor.extract(ofNullable(basis).orElseGet(Map::of), ApplicantRole.APPLICANT);
			final var warning = StudiehjalpSeptemberRule.evaluate(applicationMonth, incomes);

			LOG.info("Studiehjälp september rule evaluated (warning: {})", warning.isPresent());
			return warning;
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the wider SSBTEK window for the studiehjälp september rule - skipping it this run", e);
			return Optional.empty();
		}
	}

	private static String append(final String existing, final Optional<String> warning) {
		return warning
			.map(text -> {
				if (existing.isBlank()) {
					return text;
				}
				return existing + WARNING_SEPARATOR + text;
			})
			.orElse(existing);
	}
}
