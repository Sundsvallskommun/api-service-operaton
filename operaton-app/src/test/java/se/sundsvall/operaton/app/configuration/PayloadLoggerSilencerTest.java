package se.sundsvall.operaton.app.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadLoggerSilencerTest {

	private static final String LOGGER_LEVEL_KEY = "logging.level.se.sundsvall.dept44.payload";
	private static final String PROPERTY_SOURCE_NAME = "operatonPayloadLoggerOverride";

	@Test
	void postProcessEnvironment_setsPayloadLoggerOff() {
		final var environment = new MockEnvironment();
		environment.setProperty(LOGGER_LEVEL_KEY, "TRACE");

		new PayloadLoggerSilencer().postProcessEnvironment(environment, null);

		assertThat(environment.getProperty(LOGGER_LEVEL_KEY)).isEqualTo("OFF");
	}

	@Test
	void postProcessEnvironment_addsOverrideAtTopOfPropertySources() {
		final var environment = new MockEnvironment();

		new PayloadLoggerSilencer().postProcessEnvironment(environment, null);

		assertThat(environment.getPropertySources().iterator().next().getName()).isEqualTo(PROPERTY_SOURCE_NAME);
	}
}
