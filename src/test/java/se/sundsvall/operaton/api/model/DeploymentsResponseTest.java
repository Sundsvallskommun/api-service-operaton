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

class DeploymentsResponseTest {

	@Test
	void testBean() {
		assertThat(DeploymentsResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var deployments = List.of(DeploymentResponse.create().withId("deploy-1"));

		final var result = DeploymentsResponse.create()
			.withDeployments(deployments);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getDeployments()).isEqualTo(deployments);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DeploymentsResponse.create()).hasAllNullFieldsOrProperties();
	}
}
