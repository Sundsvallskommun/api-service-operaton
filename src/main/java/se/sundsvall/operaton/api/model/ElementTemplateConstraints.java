package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Validation constraints for an element template property")
public class ElementTemplateConstraints {

	@Schema(description = "Whether the value must not be empty", examples = "true")
	private Boolean notEmpty;

	public static ElementTemplateConstraints create() {
		return new ElementTemplateConstraints();
	}

	public Boolean getNotEmpty() {
		return notEmpty;
	}

	public void setNotEmpty(final Boolean notEmpty) {
		this.notEmpty = notEmpty;
	}

	public ElementTemplateConstraints withNotEmpty(final Boolean notEmpty) {
		this.notEmpty = notEmpty;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ElementTemplateConstraints that = (ElementTemplateConstraints) o;
		return Objects.equals(notEmpty, that.notEmpty);
	}

	@Override
	public int hashCode() {
		return Objects.hash(notEmpty);
	}

	@Override
	public String toString() {
		return "ElementTemplateConstraints{" +
			"notEmpty=" + notEmpty +
			'}';
	}
}
