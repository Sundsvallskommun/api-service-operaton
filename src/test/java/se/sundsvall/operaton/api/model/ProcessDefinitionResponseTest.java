package se.sundsvall.operaton.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ProcessDefinitionResponseTest {

	@Test
	void testBean() {
		assertThat(ProcessDefinitionResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "invoice:1:4";
		final var key = "invoice";
		final var name = "Invoice Process";
		final var version = 1;

		final var result = ProcessDefinitionResponse.create()
			.withId(id)
			.withKey(key)
			.withName(name)
			.withVersion(version);

		assertThat(result).hasNoNullFieldsOrPropertiesExcept();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessDefinitionResponse.create()).hasAllNullFieldsOrPropertiesExcept("version");
	}
}
