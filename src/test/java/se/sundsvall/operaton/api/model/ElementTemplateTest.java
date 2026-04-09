package se.sundsvall.operaton.api.model;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ElementTemplateTest {

	@BeforeAll
	static void setup() {
		final var counter = new java.util.concurrent.atomic.AtomicLong();
		registerValueGenerator(() -> ElementTemplateProperty.create()
			.withLabel("label-" + counter.incrementAndGet())
			.withType("type-" + counter.incrementAndGet())
			.withBinding(ElementTemplateBinding.create()
				.withType("bt-" + counter.incrementAndGet())
				.withName("bn-" + counter.incrementAndGet())),
			ElementTemplateProperty.class);
	}

	@Test
	void testBean() {
		assertThat(ElementTemplate.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var schema = "https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json";
		final var id = "se.sundsvall.operaton.SendEmail";
		final var name = "Send Email";
		final var description = "Sends an email via the Messaging API";
		final var appliesTo = List.of("bpmn:ServiceTask");
		final var properties = List.of(ElementTemplateProperty.create().withLabel("Email").withType("String"));

		final var result = ElementTemplate.create()
			.withSchema(schema)
			.withId(id)
			.withName(name)
			.withDescription(description)
			.withAppliesTo(appliesTo)
			.withProperties(properties);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getSchema()).isEqualTo(schema);
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getAppliesTo()).isEqualTo(appliesTo);
		assertThat(result.getProperties()).isEqualTo(properties);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ElementTemplate.create()).hasAllNullFieldsOrProperties();
	}
}
