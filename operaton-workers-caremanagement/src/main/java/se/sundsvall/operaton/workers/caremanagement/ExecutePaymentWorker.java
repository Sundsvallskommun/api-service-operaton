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
 * Executes the payment (utbetalning) for an approved ekonomiskt-bistånd errand and reports whether it was effectuated
 * via the {@code paymentEffectuated} output variable, which the rakel-ekonomiskt-bistand process gates on.
 *
 * <p>
 * STUB: there is no payment/RPA/Lifecare endpoint yet, so the worker currently reports the payment as effectuated so
 * the
 * process can proceed to UTBETALD. When a payment integration exists, trigger it here and derive
 * {@code paymentEffectuated} from its result.
 */
@Component
@TopicWorker(
	topic = "execute-payment",
	description = "Executes the payment (utbetalning) for an approved ekonomiskt-bistånd errand and reports whether it was effectuated. STUB: no payment/RPA/Lifecare endpoint exists yet — the worker reports the payment as effectuated so the process can proceed; wire it to the real payment call once available.",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		ExecutePaymentWorker.VAR_NAMESPACE,
		ExecutePaymentWorker.VAR_ERRAND_ID,
		ExecutePaymentWorker.VAR_CALCULATION_ID
	},
	outputVariables = {
		ExecutePaymentWorker.VAR_OUT_PAYMENT_EFFECTUATED
	})
public class ExecutePaymentWorker extends AbstractTopicWorker {

	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_ERRAND_ID = "errandId";
	static final String VAR_CALCULATION_ID = "calculationId";

	static final String VAR_OUT_PAYMENT_EFFECTUATED = "paymentEffectuated";

	private static final Logger LOG = LoggerFactory.getLogger(ExecutePaymentWorker.class);

	public ExecutePaymentWorker(final ExternalTaskService externalTaskService) {
		super(externalTaskService);
	}

	@Dept44Scheduled(cron = "${scheduler.execute-payment.cron:*/5 * * * * *}", name = "execute-payment-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var errandId = requireVariable(task, VAR_ERRAND_ID, String.class);

		// STUB: when a payment/RPA/Lifecare endpoint exists, trigger the payment here and
		// derive paymentEffectuated from its result.

		LOG.info("Payment step completed for errand {}", sanitizeForLogging(errandId));
		return Map.of(VAR_OUT_PAYMENT_EFFECTUATED, true);
	}
}
