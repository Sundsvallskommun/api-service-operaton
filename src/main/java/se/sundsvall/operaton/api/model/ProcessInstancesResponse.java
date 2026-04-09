package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Process instances response model")
public class ProcessInstancesResponse {

	@ArraySchema(schema = @Schema(implementation = ProcessInstanceResponse.class))
	private List<ProcessInstanceResponse> processInstances;

	public static ProcessInstancesResponse create() {
		return new ProcessInstancesResponse();
	}

	public List<ProcessInstanceResponse> getProcessInstances() {
		return processInstances;
	}

	public void setProcessInstances(final List<ProcessInstanceResponse> processInstances) {
		this.processInstances = processInstances;
	}

	public ProcessInstancesResponse withProcessInstances(final List<ProcessInstanceResponse> processInstances) {
		this.processInstances = processInstances;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ProcessInstancesResponse that = (ProcessInstancesResponse) o;
		return Objects.equals(processInstances, that.processInstances);
	}

	@Override
	public int hashCode() {
		return Objects.hash(processInstances);
	}

	@Override
	public String toString() {
		return "ProcessInstancesResponse{" +
			"processInstances=" + processInstances +
			'}';
	}
}
