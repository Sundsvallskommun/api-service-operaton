package se.sundsvall.operaton.workers.api.model;

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

class TopicDescriptionTest {

	@Test
	void testBean() {
		assertThat(TopicDescription.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var topic = "send-email";
		final var description = "Sends an email";
		final var inputVariables = List.of("to", "subject");
		final var outputVariables = List.of("messageId");

		final var result = TopicDescription.create()
			.withTopic(topic)
			.withDescription(description)
			.withInputVariables(inputVariables)
			.withOutputVariables(outputVariables);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getTopic()).isEqualTo(topic);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getInputVariables()).isEqualTo(inputVariables);
		assertThat(result.getOutputVariables()).isEqualTo(outputVariables);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TopicDescription.create()).hasAllNullFieldsOrProperties();
	}
}
