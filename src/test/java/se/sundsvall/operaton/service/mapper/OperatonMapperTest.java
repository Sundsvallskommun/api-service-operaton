package se.sundsvall.operaton.service.mapper;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatonMapperTest {

	@Test
	void toDeploymentResponse() {
		final var deployment = mock(Deployment.class);
		when(deployment.getId()).thenReturn("deploy-1");
		when(deployment.getName()).thenReturn("test");
		when(deployment.getSource()).thenReturn("api");
		when(deployment.getDeploymentTime()).thenReturn(new Date());

		final var result = OperatonMapper.toDeploymentResponse(deployment);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("deploy-1");
		assertThat(result.getName()).isEqualTo("test");
		assertThat(result.getSource()).isEqualTo("api");
		assertThat(result.getDeploymentTime()).isNotNull();
	}

	@Test
	void toDeploymentResponseWithNull() {
		assertThat(OperatonMapper.toDeploymentResponse(null)).isNull();
	}

	@Test
	void toDeploymentResponseWithNullDeploymentTime() {
		final var deployment = mock(Deployment.class);
		when(deployment.getId()).thenReturn("deploy-1");

		final var result = OperatonMapper.toDeploymentResponse(deployment);

		assertThat(result).isNotNull();
		assertThat(result.getDeploymentTime()).isNull();
	}

	@Test
	void toDeploymentsResponse() {
		final var deployment = mock(Deployment.class);
		when(deployment.getId()).thenReturn("deploy-1");
		when(deployment.getName()).thenReturn("test");
		when(deployment.getDeploymentTime()).thenReturn(new Date());

		final var result = OperatonMapper.toDeploymentsResponse(List.of(deployment));

		assertThat(result).isNotNull();
		assertThat(result.getDeployments()).hasSize(1);
		assertThat(result.getDeployments().getFirst().getId()).isEqualTo("deploy-1");
	}

	@Test
	void toDeploymentsResponseWithNull() {
		final var result = OperatonMapper.toDeploymentsResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getDeployments()).isEmpty();
	}

	@Test
	void toProcessDefinitionResponse() {
		final var pd = mock(ProcessDefinition.class);
		when(pd.getId()).thenReturn("invoice:1:4");
		when(pd.getKey()).thenReturn("invoice");
		when(pd.getName()).thenReturn("Invoice Process");
		when(pd.getVersion()).thenReturn(1);
		when(pd.getDeploymentId()).thenReturn("deploy-42");

		final var result = OperatonMapper.toProcessDefinitionResponse(pd);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("invoice:1:4");
		assertThat(result.getKey()).isEqualTo("invoice");
		assertThat(result.getName()).isEqualTo("Invoice Process");
		assertThat(result.getVersion()).isEqualTo(1);
		assertThat(result.getDeploymentId()).isEqualTo("deploy-42");
	}

	@Test
	void toProcessDefinitionResponseWithNull() {
		assertThat(OperatonMapper.toProcessDefinitionResponse(null)).isNull();
	}

	@Test
	void toProcessDefinitionsResponse() {
		final var pd = mock(ProcessDefinition.class);
		when(pd.getId()).thenReturn("invoice:1:4");
		when(pd.getKey()).thenReturn("invoice");

		final var result = OperatonMapper.toProcessDefinitionsResponse(List.of(pd));

		assertThat(result).isNotNull();
		assertThat(result.getProcessDefinitions()).hasSize(1);
	}

	@Test
	void toProcessDefinitionsResponseWithNull() {
		final var result = OperatonMapper.toProcessDefinitionsResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getProcessDefinitions()).isEmpty();
	}

	@Test
	void toProcessInstanceResponse() {
		final var pi = mock(ProcessInstance.class);
		when(pi.getId()).thenReturn("pi-1");
		when(pi.getProcessDefinitionId()).thenReturn("invoice:1:4");
		when(pi.getBusinessKey()).thenReturn("order-123");
		when(pi.isSuspended()).thenReturn(false);
		when(pi.isEnded()).thenReturn(false);

		final var result = OperatonMapper.toProcessInstanceResponse(pi);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("pi-1");
		assertThat(result.getProcessDefinitionId()).isEqualTo("invoice:1:4");
		assertThat(result.getBusinessKey()).isEqualTo("order-123");
		assertThat(result.isSuspended()).isFalse();
		assertThat(result.isEnded()).isFalse();
	}

	@Test
	void toProcessInstanceResponseWithNull() {
		assertThat(OperatonMapper.toProcessInstanceResponse(null)).isNull();
	}

	@Test
	void toProcessInstancesResponse() {
		final var pi = mock(ProcessInstance.class);
		when(pi.getId()).thenReturn("pi-1");

		final var result = OperatonMapper.toProcessInstancesResponse(List.of(pi));

		assertThat(result).isNotNull();
		assertThat(result.getProcessInstances()).hasSize(1);
	}

	@Test
	void toProcessInstancesResponseWithNull() {
		final var result = OperatonMapper.toProcessInstancesResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getProcessInstances()).isEmpty();
	}
}
