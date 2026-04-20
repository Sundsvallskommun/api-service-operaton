package se.sundsvall.operaton.process.api.model;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class StartProcessInstanceRequestTest {

	@Test
	void testBean() {
		assertThat(StartProcessInstanceRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var processDefinitionKey = "invoice";
		final var businessKey = "order-12345";
		final var variables = Map.<String, Object>of("amount", 555);

		final var result = StartProcessInstanceRequest.create()
			.withProcessDefinitionKey(processDefinitionKey)
			.withBusinessKey(businessKey)
			.withVariables(variables);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getProcessDefinitionKey()).isEqualTo(processDefinitionKey);
		assertThat(result.getBusinessKey()).isEqualTo(businessKey);
		assertThat(result.getVariables()).isEqualTo(variables);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StartProcessInstanceRequest.create()).hasAllNullFieldsOrProperties();
	}
}
