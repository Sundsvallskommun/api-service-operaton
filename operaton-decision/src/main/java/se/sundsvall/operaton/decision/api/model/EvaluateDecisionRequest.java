package se.sundsvall.operaton.decision.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Objects;

@Schema(description = "Request to evaluate a decision definition against a set of input variables")
public class EvaluateDecisionRequest {

	@Schema(description = "The decision input variables (keyed by input variable name)", example = "{\"forman\":\"Bostadsbidrag\",\"delforman\":\"Bostadsbidrag\",\"beloppstyp\":\"Avdrag Soc\"}")
	private Map<String, Object> variables;

	public static EvaluateDecisionRequest create() {
		return new EvaluateDecisionRequest();
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	public void setVariables(final Map<String, Object> variables) {
		this.variables = variables;
	}

	public EvaluateDecisionRequest withVariables(final Map<String, Object> variables) {
		this.variables = variables;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final EvaluateDecisionRequest that = (EvaluateDecisionRequest) o;
		return Objects.equals(variables, that.variables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(variables);
	}

	@Override
	public String toString() {
		return "EvaluateDecisionRequest{variables=" + variables + "}";
	}
}
