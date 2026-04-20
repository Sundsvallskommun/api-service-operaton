package se.sundsvall.operaton.deployment.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Deployments response model")
public class DeploymentsResponse {

	@ArraySchema(schema = @Schema(implementation = DeploymentResponse.class))
	private List<DeploymentResponse> deployments;

	public static DeploymentsResponse create() {
		return new DeploymentsResponse();
	}

	public List<DeploymentResponse> getDeployments() {
		return deployments;
	}

	public void setDeployments(final List<DeploymentResponse> deployments) {
		this.deployments = deployments;
	}

	public DeploymentsResponse withDeployments(final List<DeploymentResponse> deployments) {
		this.deployments = deployments;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DeploymentsResponse that = (DeploymentsResponse) o;
		return Objects.equals(deployments, that.deployments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(deployments);
	}

	@Override
	public String toString() {
		return "DeploymentsResponse{" +
			"deployments=" + deployments +
			'}';
	}
}
