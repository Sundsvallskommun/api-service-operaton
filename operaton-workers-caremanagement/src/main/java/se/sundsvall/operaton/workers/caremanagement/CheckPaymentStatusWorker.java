package se.sundsvall.operaton.workers.caremanagement;

import java.util.Map;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.workers.framework.AbstractTopicWorker;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Reads whether the payment (utbetalning) for an approved ekonomiskt-bistånd errand has been effectuated and reports it
 * via the {@code paymentEffectuated} output variable that the rakel-ekonomiskt-bistand process gates on.
 *
 * <p>
 * The payment itself is a MANUAL step the handläggare performs in LifeCare — this worker makes no payment, it only
 * reads
 * the status. STUB: there is no LifeCare payment-status read yet, so the worker reports the payment as effectuated so
 * the
 * process can proceed to UTBETALD. When the read exists, return the real status (false while the manual payment is
 * still
 * pending, so the process loops on the timer until it is registered).
 */
@Component
@TopicWorker(
	topic = "check-payment-status",
	description = "Reads whether the manual payment (utbetalning) for an approved ekonomiskt-bistånd errand has been effectuated in LifeCare and reports it via paymentEffectuated. Makes NO payment — that is a manual handläggare step. STUB: no LifeCare payment-status read exists yet, so it reports the payment as effectuated; wire it to the real read once available.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		CheckPaymentStatusWorker.VAR_NAMESPACE,
		CheckPaymentStatusWorker.VAR_ERRAND_ID
	},
	outputVariables = {
		CheckPaymentStatusWorker.VAR_OUT_PAYMENT_EFFECTUATED
	})
public class CheckPaymentStatusWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";

	static final String VAR_OUT_PAYMENT_EFFECTUATED = "paymentEffectuated";

	private static final Logger LOG = LoggerFactory.getLogger(CheckPaymentStatusWorker.class);

	public CheckPaymentStatusWorker(final ExternalTaskService externalTaskService) {
		super(externalTaskService);
	}

	@Dept44Scheduled(cron = "${scheduler.check-payment-status.cron:*/5 * * * * *}", name = "check-payment-status-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);

		// STUB: when a LifeCare payment-status read exists, return the real status here
		// (false while the manual payment is still pending so the process loops on the timer).

		LOG.info("Payment status read for errand {}", sanitizeForLogging(errandId));
		return Map.of(VAR_OUT_PAYMENT_EFFECTUATED, true);
	}
}
