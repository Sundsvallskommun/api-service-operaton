package se.sundsvall.operaton.decision.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.DecisionService;
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
	private final DecisionService decisionService;

	public DmnService(final RepositoryService repositoryService, final DecisionService decisionService) {
		this.repositoryService = repositoryService;
		this.decisionService = decisionService;
	}

	/**
	 * Evaluate the latest version of a deployed decision (by key) against the given input variables and return the result
	 * rows (one map of output name → value per matched rule). Lets a runtime-published DMN — e.g. the SSBTEK
	 * inkomstregelverk rålista — be evaluated without a code change or redeploy.
	 *
	 * @param  decisionKey the decision definition key (e.g. {@code Decision_inkomstRalista})
	 * @param  variables   the decision input variables
	 * @return             the result rows; empty when no rule matched
	 */
	public List<Map<String, Object>> evaluate(final String decisionKey, final Map<String, Object> variables) {
		final var exists = repositoryService.createDecisionDefinitionQuery()
			.decisionDefinitionKey(decisionKey)
			.latestVersion()
			.count() > 0;
		if (!exists) {
			throw Problem.notFound(DECISION_DEFINITION_NOT_FOUND.formatted(decisionKey));
		}

		return decisionService.evaluateDecisionByKey(decisionKey)
			.variables(variables)
			.evaluate()
			.getResultList();
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
