package se.sundsvall.operaton.process.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Process definitions response model")
public class ProcessDefinitionsResponse {

	@ArraySchema(schema = @Schema(implementation = ProcessDefinitionResponse.class))
	private List<ProcessDefinitionResponse> processDefinitions;

	public static ProcessDefinitionsResponse create() {
		return new ProcessDefinitionsResponse();
	}

	public List<ProcessDefinitionResponse> getProcessDefinitions() {
		return processDefinitions;
	}

	public void setProcessDefinitions(final List<ProcessDefinitionResponse> processDefinitions) {
		this.processDefinitions = processDefinitions;
	}

	public ProcessDefinitionsResponse withProcessDefinitions(final List<ProcessDefinitionResponse> processDefinitions) {
		this.processDefinitions = processDefinitions;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ProcessDefinitionsResponse that = (ProcessDefinitionsResponse) o;
		return Objects.equals(processDefinitions, that.processDefinitions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(processDefinitions);
	}

	@Override
	public String toString() {
		return "ProcessDefinitionsResponse{" +
			"processDefinitions=" + processDefinitions +
			'}';
	}
}
