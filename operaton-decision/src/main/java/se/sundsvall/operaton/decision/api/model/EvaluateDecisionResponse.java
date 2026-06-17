package se.sundsvall.operaton.decision.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Schema(description = "Result of evaluating a decision definition: one entry (output name → value) per matched rule")
public class EvaluateDecisionResponse {

	@Schema(description = "The decision result rows; empty when no rule matched")
	private List<Map<String, Object>> results;

	public static EvaluateDecisionResponse create() {
		return new EvaluateDecisionResponse();
	}

	public List<Map<String, Object>> getResults() {
		return results;
	}

	public void setResults(final List<Map<String, Object>> results) {
		this.results = results;
	}

	public EvaluateDecisionResponse withResults(final List<Map<String, Object>> results) {
		this.results = results;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final EvaluateDecisionResponse that = (EvaluateDecisionResponse) o;
		return Objects.equals(results, that.results);
	}

	@Override
	public int hashCode() {
		return Objects.hash(results);
	}

	@Override
	public String toString() {
		return "EvaluateDecisionResponse{results=" + results + "}";
	}
}
