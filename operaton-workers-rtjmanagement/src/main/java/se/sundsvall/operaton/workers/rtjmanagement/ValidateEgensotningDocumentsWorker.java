package se.sundsvall.operaton.workers.rtjmanagement;

import java.util.HashMap;
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

/**
 * Validates the egensotning attachments (brandskyddskontroll + utbildningsintyg) by delegating to
 * rtj-management's document-validation endpoint, which uploads the PDFs to the Eneo LLM platform and
 * judges whether they are the correct document types, valid, and match the applicant. The boolean
 * verdict is copied into the {@code documentsValid} process variable; the BPMN auto-approve branch
 * proceeds to a decision only when it is true, otherwise the errand is routed to manual review.
 */
@Component
@TopicWorker(
	topic = "rtj-validate-egensotning-documents",
	description = "Validates the egensotning attachments with the Eneo LLM platform via rtj-management and writes the boolean verdict to process variables. A non-valid verdict routes the errand to manual review.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		ValidateEgensotningDocumentsWorker.VAR_NAMESPACE,
		ValidateEgensotningDocumentsWorker.VAR_ERRAND_ID
	},
	outputVariables = {
		ValidateEgensotningDocumentsWorker.VAR_DOCUMENTS_VALID,
		ValidateEgensotningDocumentsWorker.VAR_DOCUMENT_VALIDATION_REASON
	})
public class ValidateEgensotningDocumentsWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_DOCUMENTS_VALID = "documentsValid";
	static final String VAR_DOCUMENT_VALIDATION_REASON = "documentValidationReason";

	private static final Logger LOG = LoggerFactory.getLogger(ValidateEgensotningDocumentsWorker.class);

	private final RtjManagementClient rtjManagementClient;

	public ValidateEgensotningDocumentsWorker(final ExternalTaskService externalTaskService, final RtjManagementClient rtjManagementClient) {
		super(externalTaskService);
		this.rtjManagementClient = rtjManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.rtj-validate-egensotning-documents.cron:*/5 * * * * *}", name = "rtj-validate-egensotning-documents-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);
		final var result = rtjManagementClient.validateDocuments(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			errandId);

		final var valid = Boolean.TRUE.equals(result.getValid());

		final Map<String, Object> output = new HashMap<>();
		output.put(VAR_DOCUMENTS_VALID, valid);
		ofNullable(result.getReason()).ifPresent(reason -> output.put(VAR_DOCUMENT_VALIDATION_REASON, reason));

		LOG.info("Validated egensotning documents for errand {}: documentsValid={}", sanitizeForLogging(errandId), valid);
		return output;
	}
}
