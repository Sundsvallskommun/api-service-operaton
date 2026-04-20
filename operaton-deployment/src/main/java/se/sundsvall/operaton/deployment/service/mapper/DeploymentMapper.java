package se.sundsvall.operaton.deployment.service.mapper;

import java.time.ZoneId;
import java.util.List;
import org.operaton.bpm.engine.repository.Deployment;
import se.sundsvall.operaton.deployment.api.model.DeploymentResponse;
import se.sundsvall.operaton.deployment.api.model.DeploymentsResponse;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class DeploymentMapper {

	private DeploymentMapper() {}

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
				.map(DeploymentMapper::toDeploymentResponse)
				.toList());
	}
}
