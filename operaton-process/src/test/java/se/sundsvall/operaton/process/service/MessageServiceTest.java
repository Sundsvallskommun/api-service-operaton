package se.sundsvall.operaton.process.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.MessageCorrelationBuilder;
import org.operaton.bpm.engine.runtime.MessageCorrelationResult;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.operaton.process.api.model.CorrelationMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private RuntimeService runtimeServiceMock;

	@InjectMocks
	private MessageService messageService;

	@Test
	void correlateWithVariables() {
		final var builder = mock(MessageCorrelationBuilder.class);
		final var result = mock(MessageCorrelationResult.class);
		final var variables = Map.<String, Object>of("paymentDecision", "APPROVED");
		when(runtimeServiceMock.createMessageCorrelation("PaymentDecisionReceived")).thenReturn(builder);
		when(builder.processInstanceBusinessKey("errand-123")).thenReturn(builder);
		when(builder.setVariables(variables)).thenReturn(builder);
		when(builder.correlateAllWithResult()).thenReturn(List.of(result));

		final var request = CorrelationMessageRequest.create()
			.withMessageName("PaymentDecisionReceived")
			.withBusinessKey("errand-123")
			.withProcessVariables(variables);

		messageService.correlate(request);

		verify(runtimeServiceMock).createMessageCorrelation("PaymentDecisionReceived");
		verify(builder).processInstanceBusinessKey("errand-123");
		verify(builder).setVariables(variables);
		verify(builder).correlateAllWithResult();
	}

	@Test
	void correlateWithoutVariables() {
		final var builder = mock(MessageCorrelationBuilder.class);
		final var result = mock(MessageCorrelationResult.class);
		when(runtimeServiceMock.createMessageCorrelation("MessageName")).thenReturn(builder);
		when(builder.processInstanceBusinessKey("bk-1")).thenReturn(builder);
		when(builder.correlateAllWithResult()).thenReturn(List.of(result));

		final var request = CorrelationMessageRequest.create()
			.withMessageName("MessageName")
			.withBusinessKey("bk-1");

		messageService.correlate(request);

		verify(builder, never()).setVariables(anyMap());
		verify(builder).correlateAllWithResult();
	}

	@Test
	void correlateWithEmptyVariables() {
		final var builder = mock(MessageCorrelationBuilder.class);
		final var result = mock(MessageCorrelationResult.class);
		when(runtimeServiceMock.createMessageCorrelation("MessageName")).thenReturn(builder);
		when(builder.processInstanceBusinessKey("bk-1")).thenReturn(builder);
		when(builder.correlateAllWithResult()).thenReturn(List.of(result));

		final var request = CorrelationMessageRequest.create()
			.withMessageName("MessageName")
			.withBusinessKey("bk-1")
			.withProcessVariables(Map.of());

		messageService.correlate(request);

		verify(builder, never()).setVariables(anyMap());
	}

	@Test
	void correlateMultipleInstances() {
		final var builder = mock(MessageCorrelationBuilder.class);
		when(runtimeServiceMock.createMessageCorrelation("MessageName")).thenReturn(builder);
		when(builder.processInstanceBusinessKey("bk-many")).thenReturn(builder);
		when(builder.correlateAllWithResult()).thenReturn(List.of(
			mock(MessageCorrelationResult.class), mock(MessageCorrelationResult.class)));

		final var request = CorrelationMessageRequest.create()
			.withMessageName("MessageName")
			.withBusinessKey("bk-many");

		messageService.correlate(request);

		verify(builder).correlateAllWithResult();
	}

	@Test
	void correlateNoMatchingProcessInstance() {
		final var builder = mock(MessageCorrelationBuilder.class);
		when(runtimeServiceMock.createMessageCorrelation("UnknownMessage")).thenReturn(builder);
		when(builder.processInstanceBusinessKey("bk-missing")).thenReturn(builder);
		when(builder.correlateAllWithResult()).thenReturn(List.of());

		final var request = CorrelationMessageRequest.create()
			.withMessageName("UnknownMessage")
			.withBusinessKey("bk-missing");

		final var exception = assertThrows(ThrowableProblem.class, () -> messageService.correlate(request));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getMessage()).contains("UnknownMessage").contains("bk-missing");
	}

	@Test
	void correlateEngineFailureTranslatedToInternalServerError() {
		final var builder = mock(MessageCorrelationBuilder.class);
		when(runtimeServiceMock.createMessageCorrelation("MessageName")).thenReturn(builder);
		when(builder.processInstanceBusinessKey(any())).thenReturn(builder);
		when(builder.correlateAllWithResult()).thenThrow(new ProcessEngineException("engine boom"));

		final var request = CorrelationMessageRequest.create()
			.withMessageName("MessageName")
			.withBusinessKey("bk-1");

		final var exception = assertThrows(ThrowableProblem.class, () -> messageService.correlate(request));

		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getMessage()).contains("engine boom");
	}
}
