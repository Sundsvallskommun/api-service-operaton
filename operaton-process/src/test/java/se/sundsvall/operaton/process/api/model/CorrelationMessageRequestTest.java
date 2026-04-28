package se.sundsvall.operaton.process.api.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
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

class CorrelationMessageRequestTest {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		assertThat(CorrelationMessageRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var messageName = "PaymentDecisionReceived";
		final var businessKey = "errand-123";
		final var variables = Map.<String, Object>of("paymentDecision", "APPROVED");

		final var result = CorrelationMessageRequest.create()
			.withMessageName(messageName)
			.withBusinessKey(businessKey)
			.withProcessVariables(variables);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getMessageName()).isEqualTo(messageName);
		assertThat(result.getBusinessKey()).isEqualTo(businessKey);
		assertThat(result.getProcessVariables()).isEqualTo(variables);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CorrelationMessageRequest.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatesRequiredFields() {
		final var violations = VALIDATOR.validate(CorrelationMessageRequest.create());

		assertThat(violations).extracting(v -> v.getPropertyPath().toString())
			.containsExactlyInAnyOrder("messageName", "businessKey");
	}

	@Test
	void rejectsBlankMessageName() {
		final var violations = VALIDATOR.validate(CorrelationMessageRequest.create()
			.withMessageName("   ")
			.withBusinessKey("bk-1"));

		assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("messageName");
	}

	@Test
	void rejectsBlankBusinessKey() {
		final var violations = VALIDATOR.validate(CorrelationMessageRequest.create()
			.withMessageName("MessageName")
			.withBusinessKey(""));

		assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("businessKey");
	}

	@Test
	void acceptsRequestWithoutVariables() {
		final var violations = VALIDATOR.validate(CorrelationMessageRequest.create()
			.withMessageName("MessageName")
			.withBusinessKey("bk-1"));

		assertThat(violations).isEmpty();
	}
}
