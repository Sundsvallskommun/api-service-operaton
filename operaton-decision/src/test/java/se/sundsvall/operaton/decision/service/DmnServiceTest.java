package se.sundsvall.operaton.decision.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinitionQuery;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DmnServiceTest {

	@Mock
	private RepositoryService repositoryServiceMock;

	@InjectMocks
	private DmnService dmnService;

	@Test
	void getDecisionDefinitions() {
		final var query = mock(DecisionDefinitionQuery.class);
		final var decisionDefinition = mock(DecisionDefinition.class);

		when(repositoryServiceMock.createDecisionDefinitionQuery()).thenReturn(query);
		when(query.latestVersion()).thenReturn(query);
		when(query.list()).thenReturn(List.of(decisionDefinition));
		when(decisionDefinition.getId()).thenReturn("approve-loan:1:5");
		when(decisionDefinition.getKey()).thenReturn("approve-loan");
		when(decisionDefinition.getName()).thenReturn("Approve Loan");
		when(decisionDefinition.getVersion()).thenReturn(1);

		final var result = dmnService.getDecisionDefinitions();

		assertThat(result).isNotNull();
		assertThat(result.getDecisionDefinitions()).hasSize(1);
		assertThat(result.getDecisionDefinitions().getFirst().getKey()).isEqualTo("approve-loan");
		verify(repositoryServiceMock).createDecisionDefinitionQuery();
	}

	@Test
	void getDecisionDefinition() {
		final var dd = mock(DecisionDefinition.class);
		when(dd.getId()).thenReturn("approve-loan:1:5");
		when(dd.getKey()).thenReturn("approve-loan");
		when(dd.getName()).thenReturn("Approve Loan");
		when(dd.getVersion()).thenReturn(1);
		when(dd.getDeploymentId()).thenReturn("deploy-1");
		when(repositoryServiceMock.getDecisionDefinition("approve-loan:1:5")).thenReturn(dd);

		final var result = dmnService.getDecisionDefinition("approve-loan:1:5");

		assertThat(result.getId()).isEqualTo("approve-loan:1:5");
		assertThat(result.getKey()).isEqualTo("approve-loan");
		assertThat(result.getDeploymentId()).isEqualTo("deploy-1");
		verify(repositoryServiceMock).getDecisionDefinition("approve-loan:1:5");
	}

	@Test
	void getDecisionDefinitionNotFound() {
		when(repositoryServiceMock.getDecisionDefinition("non-existent"))
			.thenThrow(new ProcessEngineException("not found"));

		final var exception = assertThrows(ThrowableProblem.class,
			() -> dmnService.getDecisionDefinition("non-existent"));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getMessage()).contains("non-existent");
	}

	@Test
	void getDecisionModel() {
		final var xml = "<dmn>test</dmn>".getBytes();
		when(repositoryServiceMock.getDecisionModel("approve-loan:1:5"))
			.thenReturn(new ByteArrayInputStream(xml));

		final var result = dmnService.getDecisionModel("approve-loan:1:5");

		assertThat(result).isEqualTo(xml);
		verify(repositoryServiceMock).getDecisionModel("approve-loan:1:5");
	}

	@Test
	void getDecisionModelNotFound() {
		when(repositoryServiceMock.getDecisionModel("non-existent"))
			.thenThrow(new ProcessEngineException("not found"));

		final var exception = assertThrows(ThrowableProblem.class,
			() -> dmnService.getDecisionModel("non-existent"));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getMessage()).contains("non-existent");
	}

	@Test
	void getDecisionModelIoException() {
		final var brokenStream = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("boom");
			}
		};
		when(repositoryServiceMock.getDecisionModel("approve-loan:1:5")).thenReturn(brokenStream);

		final var exception = assertThrows(ThrowableProblem.class,
			() -> dmnService.getDecisionModel("approve-loan:1:5"));

		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getMessage()).contains("boom");
	}
}
