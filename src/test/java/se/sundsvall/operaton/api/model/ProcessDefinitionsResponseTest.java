package se.sundsvall.operaton.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ProcessDefinitionsResponseTest {

	@Test
	void testBean() {
		assertThat(ProcessDefinitionsResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var processDefinitions = List.of(ProcessDefinitionResponse.create().withId("invoice:1:4"));

		final var result = ProcessDefinitionsResponse.create()
			.withProcessDefinitions(processDefinitions);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getProcessDefinitions()).isEqualTo(processDefinitions);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessDefinitionsResponse.create()).hasAllNullFieldsOrProperties();
	}
}
