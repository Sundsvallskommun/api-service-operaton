package se.sundsvall.operaton.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DeploymentResponseTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DeploymentResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "12345";
		final var name = "invoice-process";
		final var source = "test";
		final var deploymentTime = OffsetDateTime.now();

		final var result = DeploymentResponse.create()
			.withId(id)
			.withName(name)
			.withSource(source)
			.withDeploymentTime(deploymentTime);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getSource()).isEqualTo(source);
		assertThat(result.getDeploymentTime()).isEqualTo(deploymentTime);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DeploymentResponse.create()).hasAllNullFieldsOrProperties();
	}
}
