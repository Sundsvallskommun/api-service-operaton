package se.sundsvall.operaton.process.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Process instance response model")
public class ProcessInstanceResponse {

	@Schema(description = "Process instance ID", examples = "a-process-instance-id")
	private String id;

	@Schema(description = "Process definition ID", examples = "invoice:1:4")
	private String processDefinitionId;

	@Schema(description = "Business key", examples = "order-12345")
	private String businessKey;

	@Schema(description = "Whether the process instance is suspended")
	private boolean suspended;

	@Schema(description = "Whether the process instance has ended")
	private boolean ended;

	public static ProcessInstanceResponse create() {
		return new ProcessInstanceResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ProcessInstanceResponse withId(final String id) {
		this.id = id;
		return this;
	}

	public String getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(final String processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public ProcessInstanceResponse withProcessDefinitionId(final String processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
		return this;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
	}

	public ProcessInstanceResponse withBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
		return this;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(final boolean suspended) {
		this.suspended = suspended;
	}

	public ProcessInstanceResponse withSuspended(final boolean suspended) {
		this.suspended = suspended;
		return this;
	}

	public boolean isEnded() {
		return ended;
	}

	public void setEnded(final boolean ended) {
		this.ended = ended;
	}

	public ProcessInstanceResponse withEnded(final boolean ended) {
		this.ended = ended;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ProcessInstanceResponse that = (ProcessInstanceResponse) o;
		return suspended == that.suspended && ended == that.ended && Objects.equals(id, that.id) && Objects.equals(processDefinitionId, that.processDefinitionId) && Objects.equals(businessKey, that.businessKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, processDefinitionId, businessKey, suspended, ended);
	}

	@Override
	public String toString() {
		return "ProcessInstanceResponse{" +
			"id='" + id + '\'' +
			", processDefinitionId='" + processDefinitionId + '\'' +
			", businessKey='" + businessKey + '\'' +
			", suspended=" + suspended +
			", ended=" + ended +
			'}';
	}
}
