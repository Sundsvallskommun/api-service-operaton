package se.sundsvall.operaton.workers.financialaid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

@Component
@TopicWorker(
	topic = "fetch-financial-aid-basis",
	description = "Fetches a person's financial-aid basis (aggregated agency data from af/csn/fk/skv/so/tns/miv) from the financial-aid API and exposes the response as a JSON-string process variable for downstream tasks.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		FetchFinancialAidBasisWorker.VAR_PERSONAL_NUMBER,
		FetchFinancialAidBasisWorker.VAR_FROM_DATE,
		FetchFinancialAidBasisWorker.VAR_TO_DATE
	},
	outputVariables = {
		FetchFinancialAidBasisWorker.VAR_FINANCIAL_AID_BASIS
	})
public class FetchFinancialAidBasisWorker extends AbstractTopicWorker {

	static final String VAR_PERSONAL_NUMBER = "personalNumber";
	static final String VAR_FROM_DATE = "fromDate";
	static final String VAR_TO_DATE = "toDate";
	static final String VAR_FINANCIAL_AID_BASIS = "financialAidBasis";

	private static final Logger LOG = LoggerFactory.getLogger(FetchFinancialAidBasisWorker.class);

	private final FinancialAidClient financialAidClient;
	private final ObjectMapper objectMapper;

	public FetchFinancialAidBasisWorker(
		final ExternalTaskService externalTaskService,
		final FinancialAidClient financialAidClient,
		final ObjectMapper objectMapper) {
		super(externalTaskService);
		this.financialAidClient = financialAidClient;
		this.objectMapper = objectMapper;
	}

	@Dept44Scheduled(cron = "${scheduler.fetch-financial-aid-basis.cron:*/5 * * * * *}", name = "fetch-financial-aid-basis-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var personalNumber = requireVariable(task, VAR_PERSONAL_NUMBER, String.class);
		final var fromDate = requireVariable(task, VAR_FROM_DATE, String.class);
		final var toDate = requireVariable(task, VAR_TO_DATE, String.class);

		final var response = financialAidClient.getFinancialAidBasis(municipalityId, personalNumber, fromDate, toDate);

		final String basisJson;
		try {
			basisJson = objectMapper.writeValueAsString(response == null ? Map.of() : response);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize financial-aid basis to JSON", e);
		}

		LOG.info("Financial-aid basis fetched");
		return Map.of(VAR_FINANCIAL_AID_BASIS, basisJson);
	}
}
