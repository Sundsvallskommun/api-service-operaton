package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Binding between an element template property and a BPMN model attribute")
public class ElementTemplateBinding {

	@Schema(description = "Binding type as understood by bpmn-js-element-templates", examples = "camunda:inputParameter")
	private String type;

	@Schema(description = "Target attribute or parameter name", examples = "emailAddress")
	private String name;

	public static ElementTemplateBinding create() {
		return new ElementTemplateBinding();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ElementTemplateBinding withType(final String type) {
		this.type = type;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ElementTemplateBinding withName(final String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ElementTemplateBinding that = (ElementTemplateBinding) o;
		return Objects.equals(type, that.type) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, name);
	}

	@Override
	public String toString() {
		return "ElementTemplateBinding{" +
			"type='" + type + '\'' +
			", name='" + name + '\'' +
			'}';
	}
}
