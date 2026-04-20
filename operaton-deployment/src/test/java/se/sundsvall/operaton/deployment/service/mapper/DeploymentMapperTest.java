package se.sundsvall.operaton.deployment.service.mapper;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.repository.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeploymentMapperTest {

	@Test
	void toDeploymentResponse() {
		final var deployment = mock(Deployment.class);
		when(deployment.getId()).thenReturn("deploy-1");
		when(deployment.getName()).thenReturn("test");
		when(deployment.getSource()).thenReturn("api");
		when(deployment.getDeploymentTime()).thenReturn(new Date());

		final var result = DeploymentMapper.toDeploymentResponse(deployment);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("deploy-1");
		assertThat(result.getName()).isEqualTo("test");
		assertThat(result.getSource()).isEqualTo("api");
		assertThat(result.getDeploymentTime()).isNotNull();
	}

	@Test
	void toDeploymentResponseWithNull() {
		assertThat(DeploymentMapper.toDeploymentResponse(null)).isNull();
	}

	@Test
	void toDeploymentResponseWithNullDeploymentTime() {
		final var deployment = mock(Deployment.class);
		when(deployment.getId()).thenReturn("deploy-1");

		final var result = DeploymentMapper.toDeploymentResponse(deployment);

		assertThat(result).isNotNull();
		assertThat(result.getDeploymentTime()).isNull();
	}

	@Test
	void toDeploymentsResponse() {
		final var deployment = mock(Deployment.class);
		when(deployment.getId()).thenReturn("deploy-1");
		when(deployment.getName()).thenReturn("test");
		when(deployment.getDeploymentTime()).thenReturn(new Date());

		final var result = DeploymentMapper.toDeploymentsResponse(List.of(deployment));

		assertThat(result).isNotNull();
		assertThat(result.getDeployments()).hasSize(1);
		assertThat(result.getDeployments().getFirst().getId()).isEqualTo("deploy-1");
	}

	@Test
	void toDeploymentsResponseWithNull() {
		final var result = DeploymentMapper.toDeploymentsResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getDeployments()).isEmpty();
	}
}
