package se.sundsvall.operaton.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.operaton.api.model.StartProcessInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	@Mock
	private RepositoryService repositoryServiceMock;

	@Mock
	private RuntimeService runtimeServiceMock;

	@InjectMocks
	private ProcessService processService;

	@Test
	void getProcessDefinitions() {
		final var query = mock(ProcessDefinitionQuery.class);
		final var processDefinition = mock(ProcessDefinition.class);

		when(repositoryServiceMock.createProcessDefinitionQuery()).thenReturn(query);
		when(query.latestVersion()).thenReturn(query);
		when(query.list()).thenReturn(List.of(processDefinition));
		when(processDefinition.getId()).thenReturn("invoice:1:4");
		when(processDefinition.getKey()).thenReturn("invoice");
		when(processDefinition.getName()).thenReturn("Invoice Process");
		when(processDefinition.getVersion()).thenReturn(1);

		final var result = processService.getProcessDefinitions();

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getKey()).isEqualTo("invoice");
		verify(repositoryServiceMock).createProcessDefinitionQuery();
	}

	@Test
	void startProcessInstance() {
		final var request = StartProcessInstanceRequest.create()
			.withProcessDefinitionKey("invoice")
			.withBusinessKey("order-123")
			.withVariables(Map.of("amount", 555));

		final var processInstance = mock(ProcessInstance.class);
		when(runtimeServiceMock.startProcessInstanceByKey("invoice", "order-123", Map.of("amount", 555)))
			.thenReturn(processInstance);
		when(processInstance.getId()).thenReturn("pi-1");
		when(processInstance.getProcessDefinitionId()).thenReturn("invoice:1:4");
		when(processInstance.getBusinessKey()).thenReturn("order-123");

		final var result = processService.startProcessInstance(request);

		assertThat(result.getId()).isEqualTo("pi-1");
		assertThat(result.getBusinessKey()).isEqualTo("order-123");
		verify(runtimeServiceMock).startProcessInstanceByKey("invoice", "order-123", Map.of("amount", 555));
	}

	@Test
	void startProcessInstanceWithNullVariables() {
		final var request = StartProcessInstanceRequest.create()
			.withProcessDefinitionKey("invoice");

		final var processInstance = mock(ProcessInstance.class);
		when(runtimeServiceMock.startProcessInstanceByKey("invoice", null, Map.of()))
			.thenReturn(processInstance);
		when(processInstance.getId()).thenReturn("pi-1");

		final var result = processService.startProcessInstance(request);

		assertThat(result).isNotNull();
		verify(runtimeServiceMock).startProcessInstanceByKey("invoice", null, Map.of());
	}

	@Test
	void getProcessInstances() {
		final var query = mock(ProcessInstanceQuery.class);
		final var processInstance = mock(ProcessInstance.class);

		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(query);
		when(query.active()).thenReturn(query);
		when(query.list()).thenReturn(List.of(processInstance));
		when(processInstance.getId()).thenReturn("pi-1");

		final var result = processService.getProcessInstances();

		assertThat(result).hasSize(1);
		verify(runtimeServiceMock).createProcessInstanceQuery();
	}

	@Test
	void getProcessInstance() {
		final var query = mock(ProcessInstanceQuery.class);
		final var processInstance = mock(ProcessInstance.class);

		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(query);
		when(query.processInstanceId("pi-1")).thenReturn(query);
		when(query.singleResult()).thenReturn(processInstance);
		when(processInstance.getId()).thenReturn("pi-1");

		final var result = processService.getProcessInstance("pi-1");

		assertThat(result.getId()).isEqualTo("pi-1");
		verify(runtimeServiceMock).createProcessInstanceQuery();
	}

	@Test
	void getProcessInstanceNotFound() {
		final var query = mock(ProcessInstanceQuery.class);

		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(query);
		when(query.processInstanceId("non-existent")).thenReturn(query);
		when(query.singleResult()).thenReturn(null);

		final var exception = assertThrows(ThrowableProblem.class,
			() -> processService.getProcessInstance("non-existent"));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
	}
}
