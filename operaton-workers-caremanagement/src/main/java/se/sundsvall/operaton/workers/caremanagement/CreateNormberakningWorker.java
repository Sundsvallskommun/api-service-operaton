package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Component
@TopicWorker(
	topic = "create-normberakning",
	description = "Builds the SSBTEK-driven normberäkning for an EB application month and posts it to Lifecare via CareManagement: reads the household's SSBTEK income basis, maps it onto the applicant's Lifecare calculation proposal, and creates the normberäkning. Outputs the created calculation id and whether any income warnings need handläggare review.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CreateNormberakningWorker.VAR_NAMESPACE,
		CreateNormberakningWorker.VAR_APPLICANT,
		CreateNormberakningWorker.VAR_CO_APPLICANT,
		CreateNormberakningWorker.VAR_APPLICATION_MONTH
	})
public class CreateNormberakningWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";

	static final String VAR_OUT_CALCULATION_ID = "normberakningCalculationId";
	static final String VAR_OUT_HAS_WARNINGS = "normberakningHasWarnings";
	static final String VAR_OUT_UNHANDLED_INCOMES = "normberakningUnhandledIncomes";
	static final String VAR_OUT_CHANGE_WARNINGS = "normberakningChangeWarnings";

	private static final Logger LOG = LoggerFactory.getLogger(CreateNormberakningWorker.class);

	private final CareManagementClient careManagementClient;

	public CreateNormberakningWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.create-normberakning.cron:*/5 * * * * *}", name = "create-normberakning-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var request = new NormberakningRequest()
			.applicant(requireVariable(task, VAR_APPLICANT, String.class))
			.applicationMonth(requireVariable(task, VAR_APPLICATION_MONTH, String.class));
		optionalVariable(task, VAR_CO_APPLICANT, String.class).ifPresent(request::coApplicant);

		final var response = careManagementClient.createNormberakning(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		final var unhandledIncomes = ofNullable(response).map(NormberakningResponse::getUnhandledIncomes).orElseGet(List::of);
		final var changeWarnings = ofNullable(response).map(NormberakningResponse::getChangeWarnings).orElseGet(List::of);
		final var hasWarnings = !unhandledIncomes.isEmpty() || !changeWarnings.isEmpty();
		final var calculationId = ofNullable(response).map(NormberakningResponse::getCalculationId).orElse(null);

		final Map<String, Object> output = new HashMap<>();
		ofNullable(calculationId).ifPresent(id -> output.put(VAR_OUT_CALCULATION_ID, id));
		output.put(VAR_OUT_HAS_WARNINGS, hasWarnings);
		output.put(VAR_OUT_UNHANDLED_INCOMES, String.join("; ", unhandledIncomes));
		output.put(VAR_OUT_CHANGE_WARNINGS, String.join("; ", changeWarnings));

		LOG.info("Normberäkning {} created via CareManagement (warnings: {})", sanitizeForLogging(String.valueOf(calculationId)), hasWarnings);
		return output;
	}
}
