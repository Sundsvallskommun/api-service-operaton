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

class DecisionDefinitionsResponseTest {

	@Test
	void testBean() {
		assertThat(DecisionDefinitionsResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var decisionDefinitions = List.of(DecisionDefinitionResponse.create().withId("approve-loan:1:5"));

		final var result = DecisionDefinitionsResponse.create()
			.withDecisionDefinitions(decisionDefinitions);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getDecisionDefinitions()).isEqualTo(decisionDefinitions);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionDefinitionsResponse.create()).hasAllNullFieldsOrProperties();
	}
}
