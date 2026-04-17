package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Decision definitions response model")
public class DecisionDefinitionsResponse {

	@ArraySchema(schema = @Schema(implementation = DecisionDefinitionResponse.class))
	private List<DecisionDefinitionResponse> decisionDefinitions;

	public static DecisionDefinitionsResponse create() {
		return new DecisionDefinitionsResponse();
	}

	public List<DecisionDefinitionResponse> getDecisionDefinitions() {
		return decisionDefinitions;
	}

	public void setDecisionDefinitions(final List<DecisionDefinitionResponse> decisionDefinitions) {
		this.decisionDefinitions = decisionDefinitions;
	}

	public DecisionDefinitionsResponse withDecisionDefinitions(final List<DecisionDefinitionResponse> decisionDefinitions) {
		this.decisionDefinitions = decisionDefinitions;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DecisionDefinitionsResponse that = (DecisionDefinitionsResponse) o;
		return Objects.equals(decisionDefinitions, that.decisionDefinitions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decisionDefinitions);
	}

	@Override
	public String toString() {
		return "DecisionDefinitionsResponse{" +
			"decisionDefinitions=" + decisionDefinitions +
			'}';
	}
}
