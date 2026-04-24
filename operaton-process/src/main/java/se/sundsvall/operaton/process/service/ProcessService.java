package se.sundsvall.operaton.process.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.process.api.model.ModifyVariablesRequest;
import se.sundsvall.operaton.process.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.process.api.model.ProcessDefinitionsResponse;
import se.sundsvall.operaton.process.api.model.ProcessInstanceResponse;
import se.sundsvall.operaton.process.api.model.ProcessInstancesResponse;
import se.sundsvall.operaton.process.api.model.StartProcessInstanceRequest;
import se.sundsvall.operaton.process.service.mapper.ProcessMapper;

import static java.util.Optional.ofNullable;
import static se.sundsvall.operaton.process.service.mapper.ProcessMapper.toProcessDefinitionResponse;
import static se.sundsvall.operaton.process.service.mapper.ProcessMapper.toProcessDefinitionsResponse;
import static se.sundsvall.operaton.process.service.mapper.ProcessMapper.toProcessInstanceResponse;
import static se.sundsvall.operaton.process.service.mapper.ProcessMapper.toProcessInstancesResponse;

@Service
public class ProcessService {

	private static final String PROCESS_INSTANCE_NOT_FOUND = "Process instance with id '%s' not found";
	private static final String PROCESS_DEFINITION_NOT_FOUND = "No process definition deployed with key '%s'";
	private static final String PROCESS_DEFINITION_ID_NOT_FOUND = "Process definition with id '%s' not found";

	private final RepositoryService repositoryService;
	private final RuntimeService runtimeService;

	public ProcessService(final RepositoryService repositoryService, final RuntimeService runtimeService) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
	}

	public ProcessDefinitionsResponse getProcessDefinitions(final String name) {
		var query = repositoryService.createProcessDefinitionQuery().latestVersion();
		if (name != null) {
			query = query.processDefinitionName(name);
		}
		return toProcessDefinitionsResponse(query.list());
	}

	public ProcessInstanceResponse startProcessInstance(final StartProcessInstanceRequest request) {
		final var variables = ofNullable(request.getVariables()).orElse(Map.of());
		try {
			final var processInstance = runtimeService.startProcessInstanceByKey(
				request.getProcessDefinitionKey(),
				request.getBusinessKey(),
				variables);
			return toProcessInstanceResponse(processInstance);
		} catch (final NullValueException _) {
			throw Problem.notFound(PROCESS_DEFINITION_NOT_FOUND.formatted(request.getProcessDefinitionKey()));
		} catch (final Exception _) {
			throw Problem.internalServerError("Could not start process instance with business key '%s'", request.getBusinessKey());
		}
	}

	public ProcessInstancesResponse getProcessInstances() {
		final var instances = runtimeService.createProcessInstanceQuery()
			.active()
			.list();
		return toProcessInstancesResponse(instances);
	}

	public ProcessInstanceResponse getProcessInstance(final String id) {
		final var instance = runtimeService.createProcessInstanceQuery()
			.processInstanceId(id)
			.singleResult();
		return ofNullable(instance)
			.map(ProcessMapper::toProcessInstanceResponse)
			.orElseThrow(() -> Problem.notFound(PROCESS_INSTANCE_NOT_FOUND.formatted(id)));
	}

	public void modifyProcessInstanceVariables(final String id, final ModifyVariablesRequest request) {
		ensureProcessInstanceExists(id);
		ofNullable(request.getModifications())
			.filter(modifications -> !modifications.isEmpty())
			.ifPresent(modifications -> runtimeService.setVariables(id, modifications));
		ofNullable(request.getDeletions())
			.filter(deletions -> !deletions.isEmpty())
			.ifPresent(deletions -> runtimeService.removeVariables(id, deletions));
	}

	private void ensureProcessInstanceExists(final String id) {
		final var exists = runtimeService.createProcessInstanceQuery()
			.processInstanceId(id)
			.singleResult() != null;
		if (!exists) {
			throw Problem.notFound(PROCESS_INSTANCE_NOT_FOUND.formatted(id));
		}
	}

	public ProcessDefinitionResponse getProcessDefinition(final String processDefinitionId) {
		try {
			return toProcessDefinitionResponse(repositoryService.getProcessDefinition(processDefinitionId));
		} catch (final ProcessEngineException _) {
			throw Problem.notFound(PROCESS_DEFINITION_ID_NOT_FOUND.formatted(processDefinitionId));
		}
	}

	public byte[] getProcessModel(final String processDefinitionId) {
		try (final InputStream stream = repositoryService.getProcessModel(processDefinitionId)) {
			return stream.readAllBytes();
		} catch (final ProcessEngineException _) {
			throw Problem.notFound(PROCESS_DEFINITION_ID_NOT_FOUND.formatted(processDefinitionId));
		} catch (final IOException e) {
			throw Problem.internalServerError("Failed to read BPMN model: %s".formatted(e.getMessage()));
		}
	}
}
