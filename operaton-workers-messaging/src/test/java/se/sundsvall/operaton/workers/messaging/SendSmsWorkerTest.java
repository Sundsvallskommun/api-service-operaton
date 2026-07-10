package se.sundsvall.operaton.workers.messaging;

import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SmsRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendSmsWorkerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private MessagingClient messagingClientMock;

	@InjectMocks
	private SendSmsWorker worker;

	@Test
	void executePollsForTasks() {
		worker.execute();

		verify(externalTaskServiceMock).fetchAndLock(10, "send-sms-worker");
	}

	@Test
	void handleSendsSms() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("mobileNumber", "+46701234567")
			.putValue("message", "Test SMS")
			.putValue("sender", "TestSender"));
		when(messagingClientMock.sendSms(any(), any())).thenReturn(new MessageResult().messageId("msg-456"));

		final var result = worker.handle(task);

		final var requestCaptor = ArgumentCaptor.forClass(SmsRequest.class);
		verify(messagingClientMock).sendSms(eq("2281"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().getMobileNumber()).isEqualTo("+46701234567");
		assertThat(requestCaptor.getValue().getMessage()).isEqualTo("Test SMS");
		assertThat(requestCaptor.getValue().getSender()).isEqualTo("TestSender");
		assertThat(result).isEqualTo(Map.of("messageId", "msg-456"));
	}

	@Test
	void handleWithUnknownMessageId() {
		final var task = mock(LockedExternalTask.class);
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("mobileNumber", "+46701234567")
			.putValue("message", "Test SMS")
			.putValue("sender", "TestSender"));
		when(messagingClientMock.sendSms(any(), any())).thenReturn(new MessageResult());

		assertThat(worker.handle(task)).isEqualTo(Map.of("messageId", "unknown"));
	}

	@Test
	void handleThrowsWhenMunicipalityIdMissing() {
		final var task = mock(LockedExternalTask.class);
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> worker.handle(task))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'municipalityId' is missing on task task-1");
	}
}
