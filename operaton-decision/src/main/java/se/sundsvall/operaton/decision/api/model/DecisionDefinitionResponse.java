package se.sundsvall.operaton.decision.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Decision definition response model")
public class DecisionDefinitionResponse {

	@Schema(description = "Decision definition ID", examples = "approve-loan:1:5")
	private String id;

	@Schema(description = "Decision definition key", examples = "approve-loan")
	private String key;

	@Schema(description = "Decision definition name", examples = "Approve Loan")
	private String name;

	@Schema(description = "Decision definition version", examples = "1")
	private int version;

	@Schema(description = "ID of the deployment this decision definition belongs to", examples = "deploy-1")
	private String deploymentId;

	public static DecisionDefinitionResponse create() {
		return new DecisionDefinitionResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DecisionDefinitionResponse withId(final String id) {
		this.id = id;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public DecisionDefinitionResponse withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public DecisionDefinitionResponse withName(final String name) {
		this.name = name;
		return this;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	public DecisionDefinitionResponse withVersion(final int version) {
		this.version = version;
		return this;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(final String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public DecisionDefinitionResponse withDeploymentId(final String deploymentId) {
		this.deploymentId = deploymentId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DecisionDefinitionResponse that = (DecisionDefinitionResponse) o;
		return version == that.version && Objects.equals(id, that.id) && Objects.equals(key, that.key) && Objects.equals(name, that.name) && Objects.equals(deploymentId, that.deploymentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, name, version, deploymentId);
	}

	@Override
	public String toString() {
		return "DecisionDefinitionResponse{" +
			"id='" + id + '\'' +
			", key='" + key + '\'' +
			", name='" + name + '\'' +
			", version=" + version +
			", deploymentId='" + deploymentId + '\'' +
			'}';
	}
}
