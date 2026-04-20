package se.sundsvall.operaton.workers.service.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import se.sundsvall.operaton.workers.api.model.ElementTemplate;
import se.sundsvall.operaton.workers.api.model.ElementTemplateBinding;
import se.sundsvall.operaton.workers.api.model.ElementTemplateConstraints;
import se.sundsvall.operaton.workers.api.model.ElementTemplateProperty;
import se.sundsvall.operaton.workers.api.model.TopicDescription;

import static java.util.Optional.ofNullable;

/**
 * Converts a {@link TopicDescription} into a bpmn-js element template JSON structure.
 *
 * <p>
 * V1 generates strings-only templates: every input variable becomes a {@code String} property with a {@code notEmpty}
 * constraint. Output variables are intentionally skipped. A future iteration will enrich the {@code @TopicWorker}
 * annotation with variable types and descriptions.
 * </p>
 */
public final class ElementTemplateMapper {

	static final String SCHEMA_URL = "https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json";
	static final String ID_PREFIX = "se.sundsvall.operaton.";
	static final String APPLIES_TO_SERVICE_TASK = "bpmn:ServiceTask";
	static final String BINDING_TYPE_PROPERTY = "property";
	static final String BINDING_TYPE_INPUT_PARAMETER = "camunda:inputParameter";
	static final String BINDING_NAME_TYPE = "camunda:type";
	static final String BINDING_NAME_TOPIC = "camunda:topic";
	static final String PROPERTY_TYPE_STRING = "String";
	static final String TYPE_PROPERTY_LABEL = "Implementation";
	static final String TYPE_PROPERTY_VALUE_EXTERNAL = "external";
	static final String TOPIC_PROPERTY_LABEL = "Topic";

	private ElementTemplateMapper() {}

	public static ElementTemplate toElementTemplate(final TopicDescription topic) {
		return ofNullable(topic)
			.map(t -> ElementTemplate.create()
				.withSchema(SCHEMA_URL)
				.withId(ID_PREFIX + toPascalCase(t.getTopic()))
				.withName(humanize(t.getTopic()))
				.withDescription(t.getDescription())
				.withAppliesTo(List.of(APPLIES_TO_SERVICE_TASK))
				.withProperties(buildProperties(t)))
			.orElse(null);
	}

	private static List<ElementTemplateProperty> buildProperties(final TopicDescription topic) {
		final List<ElementTemplateProperty> properties = new ArrayList<>();
		// camunda:type="external" marks the Service Task as an external task so
		// Operaton routes it to the matching worker at runtime. Without this,
		// the task runs as a synchronous Java/expression invocation and no
		// worker is ever called.
		properties.add(ElementTemplateProperty.create()
			.withLabel(TYPE_PROPERTY_LABEL)
			.withType(PROPERTY_TYPE_STRING)
			.withValue(TYPE_PROPERTY_VALUE_EXTERNAL)
			.withEditable(Boolean.FALSE)
			.withBinding(ElementTemplateBinding.create()
				.withType(BINDING_TYPE_PROPERTY)
				.withName(BINDING_NAME_TYPE)));
		properties.add(ElementTemplateProperty.create()
			.withLabel(TOPIC_PROPERTY_LABEL)
			.withType(PROPERTY_TYPE_STRING)
			.withValue(topic.getTopic())
			.withEditable(Boolean.FALSE)
			.withBinding(ElementTemplateBinding.create()
				.withType(BINDING_TYPE_PROPERTY)
				.withName(BINDING_NAME_TOPIC)));

		ofNullable(topic.getInputVariables())
			.orElse(List.of())
			.stream()
			.map(ElementTemplateMapper::toInputVariableProperty)
			.forEach(properties::add);

		return List.copyOf(properties);
	}

	private static ElementTemplateProperty toInputVariableProperty(final String variableName) {
		return ElementTemplateProperty.create()
			.withLabel(humanize(variableName))
			.withType(PROPERTY_TYPE_STRING)
			.withBinding(ElementTemplateBinding.create()
				.withType(BINDING_TYPE_INPUT_PARAMETER)
				.withName(variableName))
			.withConstraints(ElementTemplateConstraints.create()
				.withNotEmpty(true));
	}

	/**
	 * Converts a kebab-case or camelCase identifier to a human-readable label. Examples: {@code "send-email"} →
	 * {@code "Send Email"}, {@code "emailAddress"} → {@code "Email Address"}, {@code "municipalityId"} →
	 * {@code "Municipality Id"}.
	 */
	static String humanize(final String input) {
		return ofNullable(input)
			.filter(s -> !s.isBlank())
			.map(s -> Arrays.stream(s.split("-"))
				.flatMap(ElementTemplateMapper::splitCamelCase)
				.map(ElementTemplateMapper::capitalize)
				.reduce((a, b) -> a + " " + b)
				.orElse(""))
			.orElse(null);
	}

	/**
	 * Converts a kebab-case or camelCase identifier to PascalCase. Examples: {@code "send-email"} →
	 * {@code "SendEmail"}, {@code "logMessage"} → {@code "LogMessage"}.
	 */
	static String toPascalCase(final String input) {
		return ofNullable(input)
			.filter(s -> !s.isBlank())
			.map(s -> Arrays.stream(s.split("-"))
				.flatMap(ElementTemplateMapper::splitCamelCase)
				.map(ElementTemplateMapper::capitalize)
				.reduce("", String::concat))
			.orElse(null);
	}

	private static Stream<String> splitCamelCase(final String input) {
		return Arrays.stream(input.split("(?<!^)(?=[A-Z])"));
	}

	private static String capitalize(final String input) {
		if (input.isEmpty()) {
			return input;
		}
		return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
	}
}
