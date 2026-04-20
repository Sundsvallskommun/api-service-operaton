package se.sundsvall.operaton.decision.service.mapper;

import java.util.List;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionResponse;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionsResponse;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class DecisionMapper {

	private DecisionMapper() {}

	public static DecisionDefinitionResponse toDecisionDefinitionResponse(final DecisionDefinition decisionDefinition) {
		return ofNullable(decisionDefinition)
			.map(dd -> DecisionDefinitionResponse.create()
				.withId(dd.getId())
				.withKey(dd.getKey())
				.withName(dd.getName())
				.withVersion(dd.getVersion())
				.withDeploymentId(dd.getDeploymentId()))
			.orElse(null);
	}

	public static DecisionDefinitionsResponse toDecisionDefinitionsResponse(final List<DecisionDefinition> decisionDefinitions) {
		return DecisionDefinitionsResponse.create()
			.withDecisionDefinitions(ofNullable(decisionDefinitions)
				.orElse(emptyList())
				.stream()
				.map(DecisionMapper::toDecisionDefinitionResponse)
				.toList());
	}
}
