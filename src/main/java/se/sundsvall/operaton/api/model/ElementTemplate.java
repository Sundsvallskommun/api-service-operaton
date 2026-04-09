package se.sundsvall.operaton.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "A bpmn-js element template that exposes a reusable worker topic as a drag-and-drop building block in the BPMN editor")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElementTemplate {

	@Schema(description = "JSON schema the template conforms to", examples = "https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json")
	@JsonProperty("$schema")
	private String schema;

	@Schema(description = "Globally unique identifier for the template", examples = "se.sundsvall.operaton.SendEmail")
	private String id;

	@Schema(description = "Human-readable template name shown in the template chooser", examples = "Send Email")
	private String name;

	@Schema(description = "Human-readable description shown in the template chooser", examples = "Sends an email via the Messaging API")
	private String description;

	@Schema(description = "BPMN element types this template can be applied to", examples = "[\"bpmn:ServiceTask\"]")
	private List<String> appliesTo;

	@Schema(description = "Editable properties exposed by this template")
	private List<ElementTemplateProperty> properties;

	public static ElementTemplate create() {
		return new ElementTemplate();
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(final String schema) {
		this.schema = schema;
	}

	public ElementTemplate withSchema(final String schema) {
		this.schema = schema;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ElementTemplate withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ElementTemplate withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public ElementTemplate withDescription(final String description) {
		this.description = description;
		return this;
	}

	public List<String> getAppliesTo() {
		return appliesTo;
	}

	public void setAppliesTo(final List<String> appliesTo) {
		this.appliesTo = appliesTo;
	}

	public ElementTemplate withAppliesTo(final List<String> appliesTo) {
		this.appliesTo = appliesTo;
		return this;
	}

	public List<ElementTemplateProperty> getProperties() {
		return properties;
	}

	public void setProperties(final List<ElementTemplateProperty> properties) {
		this.properties = properties;
	}

	public ElementTemplate withProperties(final List<ElementTemplateProperty> properties) {
		this.properties = properties;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ElementTemplate that = (ElementTemplate) o;
		return Objects.equals(schema, that.schema) && Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(appliesTo, that.appliesTo) && Objects.equals(properties,
			that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(schema, id, name, description, appliesTo, properties);
	}

	@Override
	public String toString() {
		return "ElementTemplate{" +
			"schema='" + schema + '\'' +
			", id='" + id + '\'' +
			", name='" + name + '\'' +
			", description='" + description + '\'' +
			", appliesTo=" + appliesTo +
			", properties=" + properties +
			'}';
	}
}
