package se.sundsvall.operaton.process.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Schema(description = "Request to add, update, and/or remove variables on a running process instance")
public class ModifyVariablesRequest {

	@Schema(description = "Variables to add or update (keyed by variable name)")
	private Map<String, Object> modifications;

	@Schema(description = "Names of variables to remove")
	private List<String> deletions;

	public static ModifyVariablesRequest create() {
		return new ModifyVariablesRequest();
	}

	public Map<String, Object> getModifications() {
		return modifications;
	}

	public void setModifications(final Map<String, Object> modifications) {
		this.modifications = modifications;
	}

	public ModifyVariablesRequest withModifications(final Map<String, Object> modifications) {
		this.modifications = modifications;
		return this;
	}

	public List<String> getDeletions() {
		return deletions;
	}

	public void setDeletions(final List<String> deletions) {
		this.deletions = deletions;
	}

	public ModifyVariablesRequest withDeletions(final List<String> deletions) {
		this.deletions = deletions;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ModifyVariablesRequest that = (ModifyVariablesRequest) o;
		return Objects.equals(modifications, that.modifications) && Objects.equals(deletions, that.deletions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(modifications, deletions);
	}

	@Override
	public String toString() {
		return "ModifyVariablesRequest{" +
			"modifications=" + modifications +
			", deletions=" + deletions +
			'}';
	}
}
