package se.sundsvall.operaton.workers.messaging;

import generated.se.sundsvall.messaging.SmsRequest;
import java.util.Map;
import java.util.Optional;
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
	topic = "send-sms",
	description = "Sends an SMS via the Messaging API",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		SendSmsWorker.VAR_MOBILE_NUMBER,
		SendSmsWorker.VAR_MESSAGE,
		SendSmsWorker.VAR_SENDER
	},
	outputVariables = {
		SendSmsWorker.VAR_MESSAGE_ID
	})
public class SendSmsWorker extends AbstractTopicWorker {

	static final String VAR_MOBILE_NUMBER = "mobileNumber";
	static final String VAR_MESSAGE = "message";
	static final String VAR_SENDER = "sender";
	static final String VAR_MESSAGE_ID = "messageId";

	private static final Logger LOG = LoggerFactory.getLogger(SendSmsWorker.class);
	private static final String UNKNOWN_MESSAGE_ID = "unknown";

	private final MessagingClient messagingClient;

	public SendSmsWorker(final ExternalTaskService externalTaskService, final MessagingClient messagingClient) {
		super(externalTaskService);
		this.messagingClient = messagingClient;
	}

	@Dept44Scheduled(cron = "${scheduler.send-sms.cron:*/5 * * * * *}", name = "send-sms-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var mobileNumber = requireVariable(task, VAR_MOBILE_NUMBER, String.class);

		final var request = new SmsRequest()
			.mobileNumber(mobileNumber)
			.message(requireVariable(task, VAR_MESSAGE, String.class))
			.sender(requireVariable(task, VAR_SENDER, String.class));

		final var messageId = Optional.ofNullable(messagingClient.sendSms(municipalityId, request).getMessageId())
			.orElse(UNKNOWN_MESSAGE_ID);
		LOG.info("SMS sent to {} with messageId {}", mobileNumber, messageId);

		return Map.of(VAR_MESSAGE_ID, messageId);
	}
}
