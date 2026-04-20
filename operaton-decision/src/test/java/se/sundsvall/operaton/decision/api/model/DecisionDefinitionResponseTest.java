package se.sundsvall.operaton.decision.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DecisionDefinitionResponseTest {

	@Test
	void testBean() {
		assertThat(DecisionDefinitionResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "approve-loan:1:5";
		final var key = "approve-loan";
		final var name = "Approve Loan";
		final var version = 1;
		final var deploymentId = "deploy-1";

		final var result = DecisionDefinitionResponse.create()
			.withId(id)
			.withKey(key)
			.withName(name)
			.withVersion(version)
			.withDeploymentId(deploymentId);

		assertThat(result).hasNoNullFieldsOrPropertiesExcept();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getVersion()).isEqualTo(version);
		assertThat(result.getDeploymentId()).isEqualTo(deploymentId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionDefinitionResponse.create()).hasAllNullFieldsOrPropertiesExcept("version");
	}
}
