package se.sundsvall.operaton.process.service.mapper;

import java.util.List;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import se.sundsvall.operaton.process.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.process.api.model.ProcessDefinitionsResponse;
import se.sundsvall.operaton.process.api.model.ProcessInstanceResponse;
import se.sundsvall.operaton.process.api.model.ProcessInstancesResponse;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ProcessMapper {

	private ProcessMapper() {}

	public static ProcessDefinitionResponse toProcessDefinitionResponse(final ProcessDefinition processDefinition) {
		return ofNullable(processDefinition)
			.map(pd -> ProcessDefinitionResponse.create()
				.withId(pd.getId())
				.withKey(pd.getKey())
				.withName(pd.getName())
				.withVersion(pd.getVersion())
				.withDeploymentId(pd.getDeploymentId()))
			.orElse(null);
	}

	public static ProcessDefinitionsResponse toProcessDefinitionsResponse(final List<ProcessDefinition> processDefinitions) {
		return ProcessDefinitionsResponse.create()
			.withProcessDefinitions(ofNullable(processDefinitions)
				.orElse(emptyList())
				.stream()
				.map(ProcessMapper::toProcessDefinitionResponse)
				.toList());
	}

	public static ProcessInstanceResponse toProcessInstanceResponse(final ProcessInstance processInstance) {
		return ofNullable(processInstance)
			.map(pi -> ProcessInstanceResponse.create()
				.withId(pi.getId())
				.withProcessDefinitionId(pi.getProcessDefinitionId())
				.withBusinessKey(pi.getBusinessKey())
				.withSuspended(pi.isSuspended())
				.withEnded(pi.isEnded()))
			.orElse(null);
	}

	public static ProcessInstancesResponse toProcessInstancesResponse(final List<ProcessInstance> processInstances) {
		return ProcessInstancesResponse.create()
			.withProcessInstances(ofNullable(processInstances)
				.orElse(emptyList())
				.stream()
				.map(ProcessMapper::toProcessInstanceResponse)
				.toList());
	}
}
