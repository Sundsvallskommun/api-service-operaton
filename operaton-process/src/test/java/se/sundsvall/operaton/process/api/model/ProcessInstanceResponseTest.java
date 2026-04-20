package se.sundsvall.operaton.process.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ProcessInstanceResponseTest {

	@Test
	void testBean() {
		assertThat(ProcessInstanceResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "process-instance-id";
		final var processDefinitionId = "invoice:1:4";
		final var businessKey = "order-12345";
		final var suspended = true;
		final var ended = false;

		final var result = ProcessInstanceResponse.create()
			.withId(id)
			.withProcessDefinitionId(processDefinitionId)
			.withBusinessKey(businessKey)
			.withSuspended(suspended)
			.withEnded(ended);

		assertThat(result).hasNoNullFieldsOrPropertiesExcept();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getProcessDefinitionId()).isEqualTo(processDefinitionId);
		assertThat(result.getBusinessKey()).isEqualTo(businessKey);
		assertThat(result.isSuspended()).isEqualTo(suspended);
		assertThat(result.isEnded()).isEqualTo(ended);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessInstanceResponse.create()).hasAllNullFieldsOrPropertiesExcept("suspended", "ended");
	}
}
