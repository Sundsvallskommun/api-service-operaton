package se.sundsvall.operaton.service.mapper;

import java.time.ZoneId;
import java.util.List;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import se.sundsvall.operaton.api.model.DeploymentResponse;
import se.sundsvall.operaton.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.api.model.ProcessInstanceResponse;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class OperatonMapper {

	private OperatonMapper() {}

	public static DeploymentResponse toDeploymentResponse(final Deployment deployment) {
		return ofNullable(deployment)
			.map(d -> DeploymentResponse.create()
				.withId(d.getId())
				.withName(d.getName())
				.withSource(d.getSource())
				.withDeploymentTime(ofNullable(d.getDeploymentTime())
					.map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime())
					.orElse(null)))
			.orElse(null);
	}

	public static List<DeploymentResponse> toDeploymentResponses(final List<Deployment> deployments) {
		return ofNullable(deployments)
			.map(list -> list.stream()
				.map(OperatonMapper::toDeploymentResponse)
				.toList())
			.orElse(emptyList());
	}

	public static ProcessDefinitionResponse toProcessDefinitionResponse(final ProcessDefinition processDefinition) {
		return ofNullable(processDefinition)
			.map(pd -> ProcessDefinitionResponse.create()
				.withId(pd.getId())
				.withKey(pd.getKey())
				.withName(pd.getName())
				.withVersion(pd.getVersion()))
			.orElse(null);
	}

	public static List<ProcessDefinitionResponse> toProcessDefinitionResponses(final List<ProcessDefinition> processDefinitions) {
		return ofNullable(processDefinitions)
			.map(list -> list.stream()
				.map(OperatonMapper::toProcessDefinitionResponse)
				.toList())
			.orElse(emptyList());
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

	public static List<ProcessInstanceResponse> toProcessInstanceResponses(final List<ProcessInstance> processInstances) {
		return ofNullable(processInstances)
			.map(list -> list.stream()
				.map(OperatonMapper::toProcessInstanceResponse)
				.toList())
			.orElse(emptyList());
	}
}
