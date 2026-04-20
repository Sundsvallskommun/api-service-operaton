package se.sundsvall.operaton.decision.service;

import java.io.IOException;
import java.io.InputStream;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionResponse;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionsResponse;

import static se.sundsvall.operaton.decision.service.mapper.DecisionMapper.toDecisionDefinitionResponse;
import static se.sundsvall.operaton.decision.service.mapper.DecisionMapper.toDecisionDefinitionsResponse;

@Service
public class DmnService {

	private static final String DECISION_DEFINITION_NOT_FOUND = "Decision definition with id '%s' not found";

	private final RepositoryService repositoryService;

	public DmnService(final RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public DecisionDefinitionsResponse getDecisionDefinitions() {
		final var definitions = repositoryService.createDecisionDefinitionQuery()
			.latestVersion()
			.list();
		return toDecisionDefinitionsResponse(definitions);
	}

	public DecisionDefinitionResponse getDecisionDefinition(final String decisionDefinitionId) {
		try {
			return toDecisionDefinitionResponse(repositoryService.getDecisionDefinition(decisionDefinitionId));
		} catch (final ProcessEngineException _) {
			throw Problem.notFound(DECISION_DEFINITION_NOT_FOUND.formatted(decisionDefinitionId));
		}
	}

	public byte[] getDecisionModel(final String decisionDefinitionId) {
		try (final InputStream stream = repositoryService.getDecisionModel(decisionDefinitionId)) {
			return stream.readAllBytes();
		} catch (final ProcessEngineException _) {
			throw Problem.notFound(DECISION_DEFINITION_NOT_FOUND.formatted(decisionDefinitionId));
		} catch (final IOException e) {
			throw Problem.internalServerError("Failed to read DMN model: %s".formatted(e.getMessage()));
		}
	}
}
