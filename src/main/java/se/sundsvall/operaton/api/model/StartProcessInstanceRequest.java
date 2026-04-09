package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Objects;

@Schema(description = "Request to start a process instance")
public class StartProcessInstanceRequest {

	@Schema(description = "Process definition key", examples = "invoice")
	@NotBlank
	private String processDefinitionKey;

	@Schema(description = "Business key for the process instance", examples = "order-12345")
	private String businessKey;

	@Schema(description = "Process variables to set when starting the instance")
	private Map<String, Object> variables;

	public static StartProcessInstanceRequest create() {
		return new StartProcessInstanceRequest();
	}

	public String getProcessDefinitionKey() {
		return processDefinitionKey;
	}

	public void setProcessDefinitionKey(final String processDefinitionKey) {
		this.processDefinitionKey = processDefinitionKey;
	}

	public StartProcessInstanceRequest withProcessDefinitionKey(final String processDefinitionKey) {
		this.processDefinitionKey = processDefinitionKey;
		return this;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
	}

	public StartProcessInstanceRequest withBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
		return this;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	public void setVariables(final Map<String, Object> variables) {
		this.variables = variables;
	}

	public StartProcessInstanceRequest withVariables(final Map<String, Object> variables) {
		this.variables = variables;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final StartProcessInstanceRequest that = (StartProcessInstanceRequest) o;
		return Objects.equals(processDefinitionKey, that.processDefinitionKey) && Objects.equals(businessKey, that.businessKey) && Objects.equals(variables, that.variables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(processDefinitionKey, businessKey, variables);
	}

	@Override
	public String toString() {
		return "StartProcessInstanceRequest{" +
			"processDefinitionKey='" + processDefinitionKey + '\'' +
			", businessKey='" + businessKey + '\'' +
			", variables=" + variables +
			'}';
	}
}
