package se.sundsvall.operaton.app.configuration;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.variable.serializer.StringValueSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class VariableSizeGuardPluginTest {

	@Test
	void registersTheBoundedSerializerAheadOfTheEnginesOwn() {
		final var configuration = new StandaloneProcessEngineConfiguration();

		new VariableSizeGuardPlugin(4000).preInit(configuration);

		assertThat(configuration.getCustomPreVariableSerializers())
			.singleElement()
			.isInstanceOf(BoundedStringValueSerializer.class);
	}

	/**
	 * It has to be a <em>pre</em> serializer: the engine picks a serializer by walking the list in registration order,
	 * and custom-pre entries land before the built-ins. Registered as post it would sit behind the engine's own
	 * {@link StringValueSerializer} and never run — a guard that silently does not guard.
	 */
	@Test
	void leavesThePostSerializersAlone() {
		final var configuration = new StandaloneProcessEngineConfiguration();

		new VariableSizeGuardPlugin(4000).preInit(configuration);

		assertThat(configuration.getCustomPostVariableSerializers()).isNullOrEmpty();
	}

	@Test
	void keepsSerializersRegisteredByAnyoneElse() {
		final var configuration = new StandaloneProcessEngineConfiguration();
		final var existing = new StringValueSerializer();
		configuration.setCustomPreVariableSerializers(new ArrayList<>(List.of(existing)));

		new VariableSizeGuardPlugin(4000).preInit(configuration);

		assertThat(configuration.getCustomPreVariableSerializers())
			.hasSize(2)
			.element(0).isSameAs(existing);
	}
}
