package se.sundsvall.operaton.app.configuration;

import java.util.ArrayList;
import java.util.List;
import org.operaton.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

/**
 * Registers {@link BoundedStringValueSerializer} ahead of the engine's own, so every string variable is length-checked
 * on the way in — whichever route writes it: a worker completing a task, a process start, a message correlation, a
 * variable modification through {@code ProcessResource}, or a BPMN output mapping.
 *
 * <p>
 * Registered as a {@code customPre} serializer, which is the documented way to put a serializer ahead of a built-in:
 * the engine picks a serializer for a value by walking the list in registration order, and custom-pre entries are added
 * before the built-ins. Reading is unaffected — the class extends the built-in and changes nothing about how a stored
 * value comes back.
 *
 * <p>
 * Shaped after {@link SensitiveVariableHistoryPlugin}, and it runs in the same {@code preInit} window.
 */
@Component
public class VariableSizeGuardPlugin extends AbstractProcessEnginePlugin {

	private static final Logger LOG = LoggerFactory.getLogger(VariableSizeGuardPlugin.class);

	private final int maxStringLength;

	public VariableSizeGuardPlugin(@Value("${operaton.variables.max-string-length:4000}") final int maxStringLength) {
		this.maxStringLength = maxStringLength;
	}

	@Override
	public void preInit(final ProcessEngineConfigurationImpl configuration) {
		final var serializers = new ArrayList<>(ofNullable(configuration.getCustomPreVariableSerializers()).orElseGet(List::of));
		serializers.add(new BoundedStringValueSerializer(maxStringLength));
		configuration.setCustomPreVariableSerializers(serializers);

		LOG.info("String process variables are limited to {} characters; a longer value fails naming the variable", maxStringLength);
	}
}
