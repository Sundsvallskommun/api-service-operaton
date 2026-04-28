package se.sundsvall.operaton.process.service;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.process.api.model.CorrelationMessageRequest;

import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Service
public class MessageService {

	private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

	private static final String NO_PROCESS_INSTANCE_WAITING = "No process instance is waiting for message '%s' with business key '%s'";
	private static final String CORRELATION_FAILED = "Failed to correlate message '%s' with business key '%s': %s";

	private final RuntimeService runtimeService;

	public MessageService(final RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	public void correlate(final CorrelationMessageRequest request) {
		final var correlation = runtimeService.createMessageCorrelation(request.getMessageName())
			.processInstanceBusinessKey(request.getBusinessKey());
		ofNullable(request.getProcessVariables())
			.filter(variables -> !variables.isEmpty())
			.ifPresent(correlation::setVariables);
		try {
			final var results = correlation.correlateAllWithResult();
			if (results.isEmpty()) {
				throw Problem.notFound(NO_PROCESS_INSTANCE_WAITING.formatted(request.getMessageName(), request.getBusinessKey()));
			}
			final var sanitizedMessageName = sanitizeForLogging(request.getMessageName());
			final var sanitizedBusinessKey = sanitizeForLogging(request.getBusinessKey());
			LOG.info("Correlated message '{}' with business key '{}' to {} process instance(s)",
				sanitizedMessageName, sanitizedBusinessKey, results.size());
		} catch (final ProcessEngineException e) {
			throw Problem.internalServerError(CORRELATION_FAILED.formatted(request.getMessageName(), request.getBusinessKey(), e.getMessage()));
		}
	}
}
