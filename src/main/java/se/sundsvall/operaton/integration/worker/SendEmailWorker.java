package se.sundsvall.operaton.integration.worker;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import java.util.Map;
import java.util.Optional;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.operaton.integration.messaging.MessagingClient;
import se.sundsvall.operaton.integration.worker.annotation.TopicWorker;

@Component
@TopicWorker(
	topic = "send-email",
	description = "Sends an email via the Messaging API",
	inputVariables = {
		AbstractTopicWorker.VAR_MUNICIPALITY_ID,
		SendEmailWorker.VAR_EMAIL_ADDRESS,
		SendEmailWorker.VAR_SUBJECT,
		SendEmailWorker.VAR_MESSAGE,
		SendEmailWorker.VAR_SENDER_NAME,
		SendEmailWorker.VAR_SENDER_ADDRESS
	},
	outputVariables = {
		SendEmailWorker.VAR_MESSAGE_ID
	})
public class SendEmailWorker extends AbstractTopicWorker {

	static final String VAR_EMAIL_ADDRESS = "emailAddress";
	static final String VAR_SUBJECT = "subject";
	static final String VAR_MESSAGE = "message";
	static final String VAR_SENDER_NAME = "senderName";
	static final String VAR_SENDER_ADDRESS = "senderAddress";
	static final String VAR_MESSAGE_ID = "messageId";

	private static final Logger LOG = LoggerFactory.getLogger(SendEmailWorker.class);
	private static final String UNKNOWN_MESSAGE_ID = "unknown";

	private final MessagingClient messagingClient;

	public SendEmailWorker(final ExternalTaskService externalTaskService, final MessagingClient messagingClient) {
		super(externalTaskService);
		this.messagingClient = messagingClient;
	}

	@Dept44Scheduled(cron = "${scheduler.send-email.cron:*/5 * * * * *}", name = "send-email-worker", lockAtMostFor = "PT30S")
	public void execute() {
		processTasks();
	}

	@Override
	protected Map<String, Object> handle(final LockedExternalTask task) {
		final var municipalityId = requireVariable(task, VAR_MUNICIPALITY_ID, String.class);
		final var emailAddress = requireVariable(task, VAR_EMAIL_ADDRESS, String.class);

		final var request = new EmailRequest()
			.emailAddress(emailAddress)
			.subject(requireVariable(task, VAR_SUBJECT, String.class))
			.message(requireVariable(task, VAR_MESSAGE, String.class));

		optionalVariable(task, VAR_SENDER_NAME, String.class)
			.ifPresent(name -> request.sender(new EmailSender().name(name).address(requireVariable(task, VAR_SENDER_ADDRESS, String.class))));

		final var messageId = Optional.ofNullable(messagingClient.sendEmail(municipalityId, request).getMessageId())
			.orElse(UNKNOWN_MESSAGE_ID);
		LOG.info("Email sent to {} with messageId {}", emailAddress, messageId);

		return Map.of(VAR_MESSAGE_ID, messageId);
	}
}
