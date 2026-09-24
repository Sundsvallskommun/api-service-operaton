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
 * Reads whether the Lifecare payments of an approved financial-assistance errand have been paid out and reports it via
 * the {@code paymentEffectuated} output variable that the rakel-ekonomiskt-bistand process gates on.
 *
 * <p>
 * The worker makes no payment — Draken's BFF registers the payments directly in Lifecare. It calls CareManagement's
 * {@code financial-assistance/payment-status} endpoint with the errand, and careM reads the answer from Lifecare's own
 * payment records, errand-specifically: the Lifecare payments linked to the errand, else (for an errand decided before
 * those links) the payment rows its decision created, else the applicant's payments on the errand's own insats for the
 * application month that no other errand has taken. Another errand's payment for the same person and month never
 * counts. The errand is the {@code errandId} input variable when the model maps one, otherwise the business key
 * (businessKey = errandId), so instances already waiting on an older process version get the same check. While the
 * payments are still pending the gateway loops on the process timer; {@code paymentStatusDetail} says why, so a stuck
 * instance can be read without calling careM, and {@code paymentOverdue} turns true once careM's working-day deadline
 * after the decision has passed — the model then notifies the caseworker. Nothing is ever closed on it.
 */
@Component
@TopicWorker(
	topic = "check-payment-status",
	description = "Reads whether the Lifecare payments of an approved financial-assistance errand have been paid out (via CareManagement's errand-specific payment-status read of the Lifecare payment records) and reports it via paymentEffectuated. Makes NO payment — Draken registers it in Lifecare.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CheckPaymentStatusWorker.VAR_NAMESPACE,
		CheckPaymentStatusWorker.VAR_ERRAND_ID,
		CheckPaymentStatusWorker.VAR_APPLICANT,
		CheckPaymentStatusWorker.VAR_APPLICATION_MONTH
	},
	outputVariables = {
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_EFFECTUATED,
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_STATUS_DETAIL,
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_OVERDUE
	})
public class CheckPaymentStatusWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";

	static final String VAR_OUT_PAYMENT_EFFECTUATED = "paymentEffectuated";
	static final String VAR_OUT_PAYMENT_STATUS_DETAIL = "paymentStatusDetail";
	static final String VAR_OUT_PAYMENT_OVERDUE = "paymentOverdue";

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
		final var overdue = ofNullable(response).map(PaymentStatusResponse::getOverdue).map(TRUE::equals).orElse(false);

		LOG.info("Payment status read (effectuated: {}, overdue: {}, detail: {})", effectuated, overdue, detail);
		return Map.of(VAR_OUT_PAYMENT_EFFECTUATED, effectuated, VAR_OUT_PAYMENT_STATUS_DETAIL, detail, VAR_OUT_PAYMENT_OVERDUE, overdue);
	}
}
