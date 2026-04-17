package se.sundsvall.operaton.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

	@Mock
	private RepositoryService repositoryServiceMock;

	@InjectMocks
	private DeploymentService deploymentService;

	@Test
	void deploy() throws IOException {
		final var file = mock(MultipartFile.class);
		final var deploymentBuilder = mock(DeploymentBuilder.class);
		final var deployment = mock(Deployment.class);
		final var inputStream = new ByteArrayInputStream(new byte[0]);

		when(file.getOriginalFilename()).thenReturn("process.bpmn");
		when(file.getInputStream()).thenReturn(inputStream);
		when(repositoryServiceMock.createDeployment()).thenReturn(deploymentBuilder);
		when(deploymentBuilder.name("test-deployment")).thenReturn(deploymentBuilder);
		when(deploymentBuilder.addInputStream(eq("process.bpmn"), any())).thenReturn(deploymentBuilder);
		when(deploymentBuilder.deploy()).thenReturn(deployment);
		when(deployment.getId()).thenReturn("deploy-1");
		when(deployment.getName()).thenReturn("test-deployment");
		when(deployment.getDeploymentTime()).thenReturn(new Date());

		final var result = deploymentService.deploy("test-deployment", file);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("deploy-1");
		assertThat(result.getName()).isEqualTo("test-deployment");
		verify(repositoryServiceMock).createDeployment();
	}

	@Test
	void deployWithInvalidFile() throws IOException {
		final var file = mock(MultipartFile.class);
		final var deploymentBuilder = mock(DeploymentBuilder.class);
		final var inputStream = new ByteArrayInputStream(new byte[0]);

		when(file.getOriginalFilename()).thenReturn("broken.bpmn");
		when(file.getInputStream()).thenReturn(inputStream);
		when(repositoryServiceMock.createDeployment()).thenReturn(deploymentBuilder);
		when(deploymentBuilder.name("broken")).thenReturn(deploymentBuilder);
		when(deploymentBuilder.addInputStream(eq("broken.bpmn"), any())).thenReturn(deploymentBuilder);
		when(deploymentBuilder.deploy()).thenThrow(new ProcessEngineException("parse error at line 1"));

		final var exception = assertThrows(ThrowableProblem.class,
			() -> deploymentService.deploy("broken", file));

		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getMessage()).contains("parse error");
	}

	@Test
	void getDeployments() {
		final var deploymentQuery = mock(DeploymentQuery.class);
		final var deployment = mock(Deployment.class);

		when(repositoryServiceMock.createDeploymentQuery()).thenReturn(deploymentQuery);
		when(deploymentQuery.orderByDeploymentTime()).thenReturn(deploymentQuery);
		when(deploymentQuery.desc()).thenReturn(deploymentQuery);
		when(deploymentQuery.list()).thenReturn(List.of(deployment));
		when(deployment.getId()).thenReturn("deploy-1");
		when(deployment.getName()).thenReturn("test");
		when(deployment.getDeploymentTime()).thenReturn(new Date());

		final var result = deploymentService.getDeployments();

		assertThat(result).isNotNull();
		assertThat(result.getDeployments()).hasSize(1);
		assertThat(result.getDeployments().getFirst().getId()).isEqualTo("deploy-1");
		verify(repositoryServiceMock).createDeploymentQuery();
	}

	@Test
	void deleteDeployment() {
		doNothing().when(repositoryServiceMock).deleteDeployment("deploy-1", true);

		deploymentService.deleteDeployment("deploy-1", true);

		verify(repositoryServiceMock).deleteDeployment("deploy-1", true);
	}

	@Test
	void deleteDeploymentNotFound() {
		doThrow(new NotFoundException("not found"))
			.when(repositoryServiceMock).deleteDeployment("non-existent", false);

		final var exception = assertThrows(ThrowableProblem.class,
			() -> deploymentService.deleteDeployment("non-existent", false));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
	}

	@Test
	void deleteDeploymentWithRunningInstances() {
		doThrow(new ProcessEngineException("has running instances"))
			.when(repositoryServiceMock).deleteDeployment("deploy-1", false);

		final var exception = assertThrows(ThrowableProblem.class,
			() -> deploymentService.deleteDeployment("deploy-1", false));

		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getMessage()).contains("has running instances");
	}
}
