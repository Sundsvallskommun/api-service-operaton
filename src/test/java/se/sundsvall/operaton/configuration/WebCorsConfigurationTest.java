package se.sundsvall.operaton.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebCorsConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(WebCorsConfiguration.class);

	@Test
	void beanAbsentWhenPropertyMissing() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(WebCorsConfiguration.class));
	}

	@Test
	void beanAbsentWhenPropertyEmpty() {
		contextRunner
			.withPropertyValues("operaton.cors.allowed-origins=")
			.run(context -> assertThat(context).doesNotHaveBean(WebCorsConfiguration.class));
	}

	@Test
	void beanPresentWhenPropertySet() {
		contextRunner
			.withPropertyValues("operaton.cors.allowed-origins=http://localhost:3000")
			.run(context -> assertThat(context).hasSingleBean(WebCorsConfiguration.class));
	}

	@Test
	void supportsMultipleOrigins() {
		contextRunner
			.withPropertyValues("operaton.cors.allowed-origins=http://localhost:3000, http://localhost:4200")
			.run(context -> assertThat(context).hasSingleBean(WebCorsConfiguration.class));
	}

	@Test
	void addCorsMappingsRegistersMapping() {
		final var registry = mock(CorsRegistry.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
		final var config = new WebCorsConfiguration("http://localhost:3000, http://localhost:4200");

		config.addCorsMappings(registry);

		verify(registry).addMapping("/**");
	}
}
