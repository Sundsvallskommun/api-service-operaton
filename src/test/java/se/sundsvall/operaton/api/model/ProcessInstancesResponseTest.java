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

class ProcessInstancesResponseTest {

	@Test
	void testBean() {
		assertThat(ProcessInstancesResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var processInstances = List.of(ProcessInstanceResponse.create().withId("pi-1"));

		final var result = ProcessInstancesResponse.create()
			.withProcessInstances(processInstances);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getProcessInstances()).isEqualTo(processInstances);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessInstancesResponse.create()).hasAllNullFieldsOrProperties();
	}
}
