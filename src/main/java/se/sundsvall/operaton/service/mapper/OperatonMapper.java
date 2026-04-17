package se.sundsvall.operaton.service.mapper;

import java.time.ZoneId;
import java.util.List;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import se.sundsvall.operaton.api.model.DecisionDefinitionResponse;
import se.sundsvall.operaton.api.model.DecisionDefinitionsResponse;
import se.sundsvall.operaton.api.model.DeploymentResponse;
import se.sundsvall.operaton.api.model.DeploymentsResponse;
import se.sundsvall.operaton.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.api.model.ProcessDefinitionsResponse;
import se.sundsvall.operaton.api.model.ProcessInstanceResponse;
import se.sundsvall.operaton.api.model.ProcessInstancesResponse;

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

	public static DeploymentsResponse toDeploymentsResponse(final List<Deployment> deployments) {
		return DeploymentsResponse.create()
			.withDeployments(ofNullable(deployments)
				.orElse(emptyList())
				.stream()
				.map(OperatonMapper::toDeploymentResponse)
				.toList());
	}

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
				.map(OperatonMapper::toProcessDefinitionResponse)
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
				.map(OperatonMapper::toDecisionDefinitionResponse)
				.toList());
	}

	public static ProcessInstancesResponse toProcessInstancesResponse(final List<ProcessInstance> processInstances) {
		return ProcessInstancesResponse.create()
			.withProcessInstances(ofNullable(processInstances)
				.orElse(emptyList())
				.stream()
				.map(OperatonMapper::toProcessInstanceResponse)
				.toList());
	}
}
