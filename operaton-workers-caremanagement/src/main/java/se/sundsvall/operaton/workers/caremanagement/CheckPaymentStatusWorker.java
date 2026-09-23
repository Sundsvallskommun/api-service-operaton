package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.PaymentStatusRequest;
import generated.se.sundsvall.caremanagement.PaymentStatusResponse;
import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

/**
 * Reads whether the manual Lifecare payment for an approved financial-assistance errand has been effectuated and
 * reports it via the {@code paymentEffectuated} output variable that the rakel-ekonomiskt-bistand process gates on.
 *
 * <p>
 * The worker makes no payment — Draken's BFF registers the decided payments in Lifecare. It calls CareManagement's
 * {@code financial-assistance/payment-status} endpoint with the errand, which verifies exactly the payments that
 * errand's decision registered, by their Lifecare ids; another payment for the same person and month does not count.
 * The errand is the {@code errandId} input variable when the model maps one, otherwise the business key (businessKey =
 * errandId), so instances already waiting on an older process version get the same check. While the payments are
 * still pending the gateway loops on the process timer; {@code paymentStatusDetail} says why, so a stuck instance can
 * be read without calling careM.
 */
@Component
@TopicWorker(
	topic = "check-payment-status",
	description = "Reads whether the manual Lifecare payment for an approved financial-assistance errand has been effectuated (via CareManagement's payment-status read of the Lifecare payment records) and reports it via paymentEffectuated. Makes NO payment — that is a manual caseworker step in Lifecare.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CheckPaymentStatusWorker.VAR_NAMESPACE,
		CheckPaymentStatusWorker.VAR_ERRAND_ID,
		CheckPaymentStatusWorker.VAR_APPLICANT,
		CheckPaymentStatusWorker.VAR_APPLICATION_MONTH
	},
	outputVariables = {
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_EFFECTUATED,
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_STATUS_DETAIL
	})
public class CheckPaymentStatusWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";

	static final String VAR_OUT_PAYMENT_EFFECTUATED = "paymentEffectuated";
	static final String VAR_OUT_PAYMENT_STATUS_DETAIL = "paymentStatusDetail";

	private static final Logger LOG = LoggerFactory.getLogger(CheckPaymentStatusWorker.class);

	private final CareManagementClient careManagementClient;

	public CheckPaymentStatusWorker(final ExternalTaskService externalTaskService, final CareManagementClient careManagementClient) {
		super(externalTaskService);
		this.careManagementClient = careManagementClient;
	}

	@Dept44Scheduled(cron = "${scheduler.check-payment-status.cron:*/5 * * * * *}", name = "check-payment-status-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var request = new PaymentStatusRequest()
			.errandId(optionalVariable(task, VAR_ERRAND_ID, String.class).orElseGet(task::getBusinessKey))
			.applicant(requireVariable(task, VAR_APPLICANT, String.class))
			.applicationMonth(requireVariable(task, VAR_APPLICATION_MONTH, String.class));

		final var response = careManagementClient.checkPaymentStatus(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		final var effectuated = ofNullable(response).map(PaymentStatusResponse::getEffectuated).map(TRUE::equals).orElse(false);
		final var detail = ofNullable(response).map(PaymentStatusResponse::getDetail).orElse("");

		LOG.info("Payment status read (effectuated: {}, detail: {})", effectuated, detail);
		return Map.of(VAR_OUT_PAYMENT_EFFECTUATED, effectuated, VAR_OUT_PAYMENT_STATUS_DETAIL, detail);
	}
}
