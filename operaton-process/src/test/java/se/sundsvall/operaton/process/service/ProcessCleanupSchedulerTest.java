package se.sundsvall.operaton.process.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessCleanupSchedulerTest {

	@Mock
	private RuntimeService runtimeServiceMock;

	@Mock
	private HistoryService historyServiceMock;

	@Mock
	private RepositoryService repositoryServiceMock; // must never be touched — deployments are preserved

	@InjectMocks
	private ProcessCleanupScheduler scheduler;

	@Test
	void resetDemoProcesses_deletesRunningThenHistoricInstances() {
		final var processInstanceQuery = mock(ProcessInstanceQuery.class);
		final var running1 = mock(ProcessInstance.class);
		final var running2 = mock(ProcessInstance.class);
		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
		when(processInstanceQuery.list()).thenReturn(List.of(running1, running2));
		when(running1.getId()).thenReturn("RPI-1");
		when(running2.getId()).thenReturn("RPI-2");

		final var historicQuery = mock(HistoricProcessInstanceQuery.class);
		final var historic1 = mock(HistoricProcessInstance.class);
		when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
		when(historicQuery.list()).thenReturn(List.of(historic1));
		when(historic1.getId()).thenReturn("HPI-1");

		scheduler.resetDemoProcesses();

		final var inOrder = inOrder(runtimeServiceMock, historyServiceMock);
		inOrder.verify(runtimeServiceMock).deleteProcessInstances(List.of("RPI-1", "RPI-2"), "nightly demo reset", true, true);
		inOrder.verify(historyServiceMock).deleteHistoricProcessInstancesBulk(List.of("HPI-1"));
	}

	@Test
	void resetDemoProcesses_neverTouchesDeployments() {
		final var processInstanceQuery = mock(ProcessInstanceQuery.class);
		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
		when(processInstanceQuery.list()).thenReturn(List.of());
		final var historicQuery = mock(HistoricProcessInstanceQuery.class);
		when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
		when(historicQuery.list()).thenReturn(List.of());

		scheduler.resetDemoProcesses();

		verifyNoInteractions(repositoryServiceMock);
	}

	@Test
	void resetDemoProcesses_noRunningInstances_skipsRuntimeDelete() {
		final var processInstanceQuery = mock(ProcessInstanceQuery.class);
		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
		when(processInstanceQuery.list()).thenReturn(List.of());
		final var historicQuery = mock(HistoricProcessInstanceQuery.class);
		when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
		when(historicQuery.list()).thenReturn(List.of());

		scheduler.resetDemoProcesses();

		verify(runtimeServiceMock, never()).deleteProcessInstances(anyList(), anyString(), anyBoolean(), anyBoolean());
		verify(historyServiceMock, never()).deleteHistoricProcessInstancesBulk(anyList());
	}

	@Test
	void resetDemoProcesses_noHistoricInstances_skipsHistoricDelete() {
		final var processInstanceQuery = mock(ProcessInstanceQuery.class);
		final var running1 = mock(ProcessInstance.class);
		when(runtimeServiceMock.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
		when(processInstanceQuery.list()).thenReturn(List.of(running1));
		when(running1.getId()).thenReturn("RPI-1");
		final var historicQuery = mock(HistoricProcessInstanceQuery.class);
		when(historyServiceMock.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
		when(historicQuery.list()).thenReturn(List.of());

		scheduler.resetDemoProcesses();

		verify(runtimeServiceMock).deleteProcessInstances(List.of("RPI-1"), "nightly demo reset", true, true);
		verify(historyServiceMock, never()).deleteHistoricProcessInstancesBulk(anyList());
	}
}
