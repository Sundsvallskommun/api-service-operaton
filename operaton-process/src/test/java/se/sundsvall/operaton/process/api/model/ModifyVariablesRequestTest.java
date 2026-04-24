package se.sundsvall.operaton.process.api.model;

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

class ModifyVariablesRequestTest {

	@Test
	void testBean() {
		assertThat(ModifyVariablesRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var modifications = Map.<String, Object>of("k", "v");
		final var deletions = List.of("oldKey");

		final var result = ModifyVariablesRequest.create()
			.withModifications(modifications)
			.withDeletions(deletions);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getModifications()).isEqualTo(modifications);
		assertThat(result.getDeletions()).isEqualTo(deletions);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ModifyVariablesRequest.create()).hasAllNullFieldsOrProperties();
	}
}
