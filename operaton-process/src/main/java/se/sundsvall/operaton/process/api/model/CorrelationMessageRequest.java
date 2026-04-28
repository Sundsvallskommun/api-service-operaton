package se.sundsvall.operaton.process.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Objects;

@Schema(description = "Request to correlate a BPMN message to a waiting receive task")
public class CorrelationMessageRequest {

	@Schema(description = "Name of the BPMN message to correlate", examples = "PaymentDecisionReceived", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String messageName;

	@Schema(description = "Business key identifying the target process instance", examples = "errand-12345", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String businessKey;

	@Schema(description = "Process variables to set when correlating the message")
	private Map<String, Object> processVariables;

	public static CorrelationMessageRequest create() {
		return new CorrelationMessageRequest();
	}

	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(final String messageName) {
		this.messageName = messageName;
	}

	public CorrelationMessageRequest withMessageName(final String messageName) {
		this.messageName = messageName;
		return this;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
	}

	public CorrelationMessageRequest withBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
		return this;
	}

	public Map<String, Object> getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariables(final Map<String, Object> processVariables) {
		this.processVariables = processVariables;
	}

	public CorrelationMessageRequest withProcessVariables(final Map<String, Object> processVariables) {
		this.processVariables = processVariables;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CorrelationMessageRequest that = (CorrelationMessageRequest) o;
		return Objects.equals(messageName, that.messageName) && Objects.equals(businessKey, that.businessKey) && Objects.equals(processVariables, that.processVariables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageName, businessKey, processVariables);
	}

	@Override
	public String toString() {
		return "CorrelationMessageRequest{" +
			"messageName='" + messageName + '\'' +
			", businessKey='" + businessKey + '\'' +
			", processVariables=" + processVariables +
			'}';
	}
}
