package se.sundsvall.operaton.decision.api.model;

import java.util.List;
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

class EvaluateDecisionResponseTest {

	@Test
	void testBean() {
		assertThat(EvaluateDecisionResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var results = List.<Map<String, Object>>of(Map.of("atgard", "TA_MED"));

		final var result = EvaluateDecisionResponse.create().withResults(results);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getResults()).isEqualTo(results);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EvaluateDecisionResponse.create()).hasAllNullFieldsOrProperties();
	}
}
