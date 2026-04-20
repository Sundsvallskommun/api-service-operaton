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

class ElementTemplateConstraintsTest {

	@Test
	void testBean() {
		assertThat(ElementTemplateConstraints.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = ElementTemplateConstraints.create()
			.withNotEmpty(true);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getNotEmpty()).isTrue();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ElementTemplateConstraints.create()).hasAllNullFieldsOrProperties();
	}
}
