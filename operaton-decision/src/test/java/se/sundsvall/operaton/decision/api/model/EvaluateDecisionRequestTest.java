package se.sundsvall.operaton.decision.api.model;

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

class EvaluateDecisionRequestTest {

	@Test
	void testBean() {
		assertThat(EvaluateDecisionRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var variables = Map.<String, Object>of("forman", "Bostadsbidrag");

		final var result = EvaluateDecisionRequest.create().withVariables(variables);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getVariables()).isEqualTo(variables);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EvaluateDecisionRequest.create()).hasAllNullFieldsOrProperties();
	}
}
