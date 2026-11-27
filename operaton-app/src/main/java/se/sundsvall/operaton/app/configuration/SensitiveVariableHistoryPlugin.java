package se.sundsvall.operaton.app.configuration;

import java.util.List;
import org.operaton.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wraps whatever history level the engine ended up with in a {@link SensitiveVariableHistoryLevel}, so the variables
 * named in {@code operaton.history.suppressed-variables} leave no history behind.
 *
 * <p>
 * Runs in {@code postInit}, <strong>after</strong> {@code initHistoryLevel()}. That is the whole point. The level is
 * taken as a resolved object rather than as the configured string, which means:
 * </p>
 * <ul>
 * <li><strong>{@code history: auto} works.</strong> Auto has no level to read at {@code preInit} — the engine derives
 * it from {@code ACT_GE_PROPERTY} during init — so an earlier version of this plugin, which mapped the configured
 * string against a table of known level names, silently did nothing on every deployment that left history at its
 * default. The configuration looked right, the payloads kept being written, and the outage it was written to fix
 * stayed open. Reading the resolved level cannot miss, whatever history is set to.</li>
 * <li><strong>It cannot stop the engine starting.</strong> The engine compares the active level's id against the one
 * stored in {@code ACT_GE_PROPERTY} and refuses to start on a disagreement, and Operaton 2.1.4 has no
 * {@code skipHistoryLevelCheck}. That comparison has already happened by {@code postInit}, and
 * {@link SensitiveVariableHistoryLevel#getId()} reports the wrapped level's id anyway, so this substitution is
 * invisible to it from either side.</li>
 * </ul>
 *
 * <p>
 * The engine reads the active level off the configuration each time it asks whether an event is produced, so replacing
 * it here takes effect for everything written afterwards. Nothing already persisted is affected — rows written before
 * this plugin worked are still there and want their own cleanup.
 * </p>
 */
@Component
public class SensitiveVariableHistoryPlugin extends AbstractProcessEnginePlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SensitiveVariableHistoryPlugin.class);

	private final List<String> suppressedVariables;

	public SensitiveVariableHistoryPlugin(
		@Value("${operaton.history.suppressed-variables:}") final List<String> suppressedVariables) {
		this.suppressedVariables = suppressedVariables.stream().filter(name -> !name.isBlank()).toList();
	}

	@Override
	public void postInit(final ProcessEngineConfigurationImpl configuration) {
		if (suppressedVariables.isEmpty()) {
			LOG.info("No variables configured for history suppression - leaving the history level alone");
			return;
		}

		final var current = configuration.getHistoryLevel();
		if (current == null) {
			// Defensive: the engine sets this during init, so reaching here means the lifecycle changed under us.
			LOG.warn("No history level resolved at postInit - {} will keep being written to history", suppressedVariables);
			return;
		}
		if (current instanceof SensitiveVariableHistoryLevel) {
			LOG.info("History level already wrapped - leaving it alone");
			return;
		}

		configuration.setHistoryLevel(new SensitiveVariableHistoryLevel(current, suppressedVariables));

		LOG.info("History level '{}' (id {}) wrapped as '{}' - no history will be written for {}",
			current.getName(), current.getId(), SensitiveVariableHistoryLevel.NAME, suppressedVariables);
	}
}
