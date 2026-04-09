package se.sundsvall.operaton.service;

import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.api.model.ProcessInstanceResponse;
import se.sundsvall.operaton.api.model.StartProcessInstanceRequest;
import se.sundsvall.operaton.service.mapper.OperatonMapper;

import static java.util.Optional.ofNullable;
import static se.sundsvall.operaton.service.mapper.OperatonMapper.toProcessDefinitionResponses;
import static se.sundsvall.operaton.service.mapper.OperatonMapper.toProcessInstanceResponse;
import static se.sundsvall.operaton.service.mapper.OperatonMapper.toProcessInstanceResponses;

@Service
public class ProcessService {

	private static final String PROCESS_INSTANCE_NOT_FOUND = "Process instance with id '%s' not found";

	private final RepositoryService repositoryService;
	private final RuntimeService runtimeService;

	public ProcessService(final RepositoryService repositoryService, final RuntimeService runtimeService) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
	}

	public List<ProcessDefinitionResponse> getProcessDefinitions() {
		final var definitions = repositoryService.createProcessDefinitionQuery()
			.latestVersion()
			.list();
		return toProcessDefinitionResponses(definitions);
	}

	public ProcessInstanceResponse startProcessInstance(final StartProcessInstanceRequest request) {
		final var variables = ofNullable(request.getVariables()).orElse(Map.of());
		final var processInstance = runtimeService.startProcessInstanceByKey(
			request.getProcessDefinitionKey(),
			request.getBusinessKey(),
			variables);
		return toProcessInstanceResponse(processInstance);
	}

	public List<ProcessInstanceResponse> getProcessInstances() {
		final var instances = runtimeService.createProcessInstanceQuery()
			.active()
			.list();
		return toProcessInstanceResponses(instances);
	}

	public ProcessInstanceResponse getProcessInstance(final String id) {
		final var instance = runtimeService.createProcessInstanceQuery()
			.processInstanceId(id)
			.singleResult();
		return ofNullable(instance)
			.map(OperatonMapper::toProcessInstanceResponse)
			.orElseThrow(() -> Problem.notFound(PROCESS_INSTANCE_NOT_FOUND.formatted(id)));
	}
}
