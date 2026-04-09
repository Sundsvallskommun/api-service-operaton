package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "Deployment response model")
public class DeploymentResponse {

	@Schema(description = "Deployment ID", examples = "12345")
	private String id;

	@Schema(description = "Deployment name", examples = "invoice-process")
	private String name;

	@Schema(description = "Deployment source")
	private String source;

	@Schema(description = "Deployment time")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime deploymentTime;

	public static DeploymentResponse create() {
		return new DeploymentResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DeploymentResponse withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public DeploymentResponse withName(final String name) {
		this.name = name;
		return this;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public DeploymentResponse withSource(final String source) {
		this.source = source;
		return this;
	}

	public OffsetDateTime getDeploymentTime() {
		return deploymentTime;
	}

	public void setDeploymentTime(final OffsetDateTime deploymentTime) {
		this.deploymentTime = deploymentTime;
	}

	public DeploymentResponse withDeploymentTime(final OffsetDateTime deploymentTime) {
		this.deploymentTime = deploymentTime;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DeploymentResponse that = (DeploymentResponse) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(source, that.source) && Objects.equals(deploymentTime, that.deploymentTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, source, deploymentTime);
	}

	@Override
	public String toString() {
		return "DeploymentResponse{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", source='" + source + '\'' +
			", deploymentTime=" + deploymentTime +
			'}';
	}
}
