package se.sundsvall.operaton.workers.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ElementTemplateBindingTest {

	@Test
	void testBean() {
		assertThat(ElementTemplateBinding.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = "camunda:inputParameter";
		final var name = "emailAddress";

		final var result = ElementTemplateBinding.create()
			.withType(type)
			.withName(name);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getName()).isEqualTo(name);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ElementTemplateBinding.create()).hasAllNullFieldsOrProperties();
	}
}
