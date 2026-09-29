package se.sundsvall.operaton.workers.framework;

import java.io.Serial;

/**
 * A task failure that no amount of retrying can fix — typically a mismatch between the workflow definition and what the
 * worker expects, such as a missing or wrongly typed process variable. {@link AbstractTopicWorker} skips the retry
 * backoff for these and lets the engine raise the incident straight away, so a modelling error surfaces immediately
 * instead of minutes later.
 */
public class NonRetryableTaskException extends IllegalStateException {

	@Serial
	private static final long serialVersionUID = 1L;

	public NonRetryableTaskException(final String message) {
		super(message);
	}
}
