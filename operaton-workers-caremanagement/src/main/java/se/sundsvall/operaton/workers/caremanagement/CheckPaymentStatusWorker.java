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
 * The payment itself is a MANUAL step the caseworker performs in Lifecare — this worker makes no payment. It calls
 * CareManagement's {@code financial-assistance/payment-status} endpoint, which reads the Lifecare payment records for
 * the applicant and application month. While the payment is still pending the gateway loops on the process timer until
 * it is registered.
 */
@Component
@TopicWorker(
	topic = "check-payment-status",
	description = "Reads whether the manual Lifecare payment for an approved financial-assistance errand has been effectuated (via CareManagement's payment-status read of the Lifecare payment records) and reports it via paymentEffectuated. Makes NO payment — that is a manual caseworker step in Lifecare.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CheckPaymentStatusWorker.VAR_NAMESPACE,
		CheckPaymentStatusWorker.VAR_APPLICANT,
		CheckPaymentStatusWorker.VAR_APPLICATION_MONTH
	},
	outputVariables = {
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_EFFECTUATED
	})
public class CheckPaymentStatusWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";

	static final String VAR_OUT_PAYMENT_EFFECTUATED = "paymentEffectuated";

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
			.applicant(requireVariable(task, VAR_APPLICANT, String.class))
			.applicationMonth(requireVariable(task, VAR_APPLICATION_MONTH, String.class));

		final var response = careManagementClient.checkPaymentStatus(
			requireVariable(task, VAR_MUNICIPALITY_ID, String.class),
			requireVariable(task, VAR_NAMESPACE, String.class),
			request).getBody();

		final var effectuated = ofNullable(response).map(PaymentStatusResponse::getEffectuated).map(TRUE::equals).orElse(false);

		LOG.info("Payment status read (effectuated: {})", effectuated);
		return Map.of(VAR_OUT_PAYMENT_EFFECTUATED, effectuated);
	}
}
