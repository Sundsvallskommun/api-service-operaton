package se.sundsvall.operaton.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A single editable property inside an element template")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElementTemplateProperty {

	@Schema(description = "Label shown in the properties panel", examples = "Email Address")
	private String label;

	@Schema(description = "Property value type", examples = "String")
	private String type;

	@Schema(description = "Default or fixed value")
	private String value;

	@Schema(description = "Whether the user may change the value (defaults to true when omitted)")
	private Boolean editable;

	@Schema(description = "How the property is bound to the BPMN model")
	private ElementTemplateBinding binding;

	@Schema(description = "Validation constraints for the property")
	private ElementTemplateConstraints constraints;

	public static ElementTemplateProperty create() {
		return new ElementTemplateProperty();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public ElementTemplateProperty withLabel(final String label) {
		this.label = label;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ElementTemplateProperty withType(final String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ElementTemplateProperty withValue(final String value) {
		this.value = value;
		return this;
	}

	public Boolean getEditable() {
		return editable;
	}

	public void setEditable(final Boolean editable) {
		this.editable = editable;
	}

	public ElementTemplateProperty withEditable(final Boolean editable) {
		this.editable = editable;
		return this;
	}

	public ElementTemplateBinding getBinding() {
		return binding;
	}

	public void setBinding(final ElementTemplateBinding binding) {
		this.binding = binding;
	}

	public ElementTemplateProperty withBinding(final ElementTemplateBinding binding) {
		this.binding = binding;
		return this;
	}

	public ElementTemplateConstraints getConstraints() {
		return constraints;
	}

	public void setConstraints(final ElementTemplateConstraints constraints) {
		this.constraints = constraints;
	}

	public ElementTemplateProperty withConstraints(final ElementTemplateConstraints constraints) {
		this.constraints = constraints;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ElementTemplateProperty that = (ElementTemplateProperty) o;
		return Objects.equals(label, that.label) && Objects.equals(type, that.type) && Objects.equals(value, that.value) && Objects.equals(editable, that.editable) && Objects.equals(binding, that.binding) && Objects.equals(constraints, that.constraints);
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, type, value, editable, binding, constraints);
	}

	@Override
	public String toString() {
		return "ElementTemplateProperty{" +
			"label='" + label + '\'' +
			", type='" + type + '\'' +
			", value='" + value + '\'' +
			", editable=" + editable +
			", binding=" + binding +
			", constraints=" + constraints +
			'}';
	}
}
