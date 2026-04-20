package se.sundsvall.operaton.workers.api.model;

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

class ElementTemplatePropertyTest {

	@BeforeAll
	static void setup() {
		final var counter = new java.util.concurrent.atomic.AtomicLong();
		registerValueGenerator(() -> ElementTemplateBinding.create()
			.withType("type-" + counter.incrementAndGet())
			.withName("name-" + counter.incrementAndGet()),
			ElementTemplateBinding.class);
		registerValueGenerator(() -> ElementTemplateConstraints.create()
			.withNotEmpty(counter.incrementAndGet() % 2 == 0),
			ElementTemplateConstraints.class);
	}

	@Test
	void testBean() {
		assertThat(ElementTemplateProperty.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var label = "Email Address";
		final var type = "String";
		final var value = "foo@example.com";
		final var editable = Boolean.FALSE;
		final var binding = ElementTemplateBinding.create().withType("camunda:inputParameter").withName("emailAddress");
		final var constraints = ElementTemplateConstraints.create().withNotEmpty(true);

		final var result = ElementTemplateProperty.create()
			.withLabel(label)
			.withType(type)
			.withValue(value)
			.withEditable(editable)
			.withBinding(binding)
			.withConstraints(constraints);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getLabel()).isEqualTo(label);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getEditable()).isEqualTo(editable);
		assertThat(result.getBinding()).isEqualTo(binding);
		assertThat(result.getConstraints()).isEqualTo(constraints);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ElementTemplateProperty.create()).hasAllNullFieldsOrProperties();
	}
}
