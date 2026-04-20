package se.sundsvall.operaton.workers.messaging;

import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendEmailWorkerTest {

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private MessagingClient messagingClientMock;

	@InjectMocks
	private SendEmailWorker sendEmailWorker;

	@Test
	void executeWithTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);
		final var messageResult = new MessageResult().messageId("msg-123");

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenReturn(Variables.createVariables()
			.putValue("municipalityId", "2281")
			.putValue("emailAddress", "test@example.com")
			.putValue("subject", "Test Subject")
			.putValue("message", "Test message"));
		when(messagingClientMock.sendEmail(any(), any())).thenReturn(messageResult);

		sendEmailWorker.execute();

		verify(messagingClientMock).sendEmail(eq("2281"), any());
		verify(externalTaskServiceMock).complete(eq("task-1"), eq("send-email-worker"), any());
	}

	@Test
	void executeWithNoTasks() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of());

		sendEmailWorker.execute();
	}

	@Test
	void executeWithException() {
		final var queryBuilder = mock(ExternalTaskQueryBuilder.class);
		final var topicBuilder = mock(ExternalTaskQueryTopicBuilder.class);
		final var task = mock(LockedExternalTask.class);

		when(externalTaskServiceMock.fetchAndLock(anyInt(), any())).thenReturn(queryBuilder);
		when(queryBuilder.topic(any(), anyLong())).thenReturn(topicBuilder);
		when(topicBuilder.execute()).thenReturn(List.of(task));
		when(task.getId()).thenReturn("task-1");
		when(task.getVariables()).thenThrow(new RuntimeException("test error"));

		sendEmailWorker.execute();

		verify(externalTaskServiceMock).handleFailure("task-1", "send-email-worker", "test error", 0, 0L);
	}
}
