package se.sundsvall.operaton.decision.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.repository.DecisionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DecisionMapperTest {

	@Test
	void toDecisionDefinitionResponse() {
		final var dd = mock(DecisionDefinition.class);
		when(dd.getId()).thenReturn("approve-loan:1:5");
		when(dd.getKey()).thenReturn("approve-loan");
		when(dd.getName()).thenReturn("Approve Loan");
		when(dd.getVersion()).thenReturn(1);
		when(dd.getDeploymentId()).thenReturn("deploy-1");

		final var result = DecisionMapper.toDecisionDefinitionResponse(dd);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("approve-loan:1:5");
		assertThat(result.getKey()).isEqualTo("approve-loan");
		assertThat(result.getName()).isEqualTo("Approve Loan");
		assertThat(result.getVersion()).isEqualTo(1);
		assertThat(result.getDeploymentId()).isEqualTo("deploy-1");
	}

	@Test
	void toDecisionDefinitionResponseWithNull() {
		assertThat(DecisionMapper.toDecisionDefinitionResponse(null)).isNull();
	}

	@Test
	void toDecisionDefinitionsResponse() {
		final var dd = mock(DecisionDefinition.class);
		when(dd.getId()).thenReturn("approve-loan:1:5");
		when(dd.getKey()).thenReturn("approve-loan");

		final var result = DecisionMapper.toDecisionDefinitionsResponse(List.of(dd));

		assertThat(result).isNotNull();
		assertThat(result.getDecisionDefinitions()).hasSize(1);
		assertThat(result.getDecisionDefinitions().getFirst().getId()).isEqualTo("approve-loan:1:5");
	}

	@Test
	void toDecisionDefinitionsResponseWithNull() {
		final var result = DecisionMapper.toDecisionDefinitionsResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getDecisionDefinitions()).isEmpty();
	}
}
