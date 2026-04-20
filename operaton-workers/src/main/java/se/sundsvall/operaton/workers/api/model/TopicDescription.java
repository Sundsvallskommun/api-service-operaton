package se.sundsvall.operaton.workers.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Description of an external task topic")
public class TopicDescription {

	@Schema(description = "Topic name", examples = "send-email")
	private String topic;

	@Schema(description = "Human-readable description of what this topic does", examples = "Sends an email via Messaging API")
	private String description;

	@Schema(description = "List of input variable names expected by the worker")
	private List<String> inputVariables;

	@Schema(description = "List of output variable names set by the worker")
	private List<String> outputVariables;

	public static TopicDescription create() {
		return new TopicDescription();
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(final String topic) {
		this.topic = topic;
	}

	public TopicDescription withTopic(final String topic) {
		this.topic = topic;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public TopicDescription withDescription(final String description) {
		this.description = description;
		return this;
	}

	public List<String> getInputVariables() {
		return inputVariables;
	}

	public void setInputVariables(final List<String> inputVariables) {
		this.inputVariables = inputVariables;
	}

	public TopicDescription withInputVariables(final List<String> inputVariables) {
		this.inputVariables = inputVariables;
		return this;
	}

	public List<String> getOutputVariables() {
		return outputVariables;
	}

	public void setOutputVariables(final List<String> outputVariables) {
		this.outputVariables = outputVariables;
	}

	public TopicDescription withOutputVariables(final List<String> outputVariables) {
		this.outputVariables = outputVariables;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final TopicDescription that = (TopicDescription) o;
		return Objects.equals(topic, that.topic) && Objects.equals(description, that.description) && Objects.equals(inputVariables, that.inputVariables) && Objects.equals(outputVariables, that.outputVariables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(topic, description, inputVariables, outputVariables);
	}

	@Override
	public String toString() {
		return "TopicDescription{" +
			"topic='" + topic + '\'' +
			", description='" + description + '\'' +
			", inputVariables=" + inputVariables +
			", outputVariables=" + outputVariables +
			'}';
	}
}
