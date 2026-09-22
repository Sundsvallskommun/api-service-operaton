package se.sundsvall.operaton.app.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

/**
 * Registers {@link SensitiveVariableHistoryLevel} and makes the engine use it, so the variables named in
 * {@code operaton.history.suppressed-variables} leave no history behind. Runs in {@code preInit}, which the engine
 * invokes before {@code initHistoryLevel()} — the only window in which a custom level can still be added to the
 * candidate list and selected.
 *
 * <p>
 * The level being wrapped is resolved from whatever {@code history} is configured rather than pinned to {@code full}.
 * That is what keeps the substitution safe: the engine refuses to start when the configured level's id disagrees with
 * the one stored in {@code ACT_GE_PROPERTY}, so taking the id from the level already in force means this plugin cannot
 * introduce a mismatch no matter how the deployment is configured.
 *
 * <p>
 * Two cases leave the engine alone and say so. {@code history: auto} has no level to wrap yet — the engine determines
 * it from the database after this point — and an unrecognised value is not ours to reinterpret. Both log a warning
 * naming the value, because silently keeping full history would put the payloads back in {@code ACT_HI_DETAIL} without
 * anyone noticing.
 */
@Component
public class SensitiveVariableHistoryPlugin extends AbstractProcessEnginePlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SensitiveVariableHistoryPlugin.class);

	private static final Map<String, HistoryLevel> WRAPPABLE_LEVELS = Map.of(
		ProcessEngineConfiguration.HISTORY_NONE, HistoryLevel.HISTORY_LEVEL_NONE,
		ProcessEngineConfiguration.HISTORY_ACTIVITY, HistoryLevel.HISTORY_LEVEL_ACTIVITY,
		ProcessEngineConfiguration.HISTORY_AUDIT, HistoryLevel.HISTORY_LEVEL_AUDIT,
		ProcessEngineConfiguration.HISTORY_FULL, HistoryLevel.HISTORY_LEVEL_FULL);

	private final List<String> suppressedVariables;

	public SensitiveVariableHistoryPlugin(
		@Value("${operaton.history.suppressed-variables:}") final List<String> suppressedVariables) {
		this.suppressedVariables = suppressedVariables.stream().filter(name -> !name.isBlank()).toList();
	}

	@Override
	public void preInit(final ProcessEngineConfigurationImpl configuration) {
		if (suppressedVariables.isEmpty()) {
			LOG.info("No variables configured for history suppression - leaving the history level alone");
			return;
		}

		final var history = ofNullable(configuration.getHistory()).orElse("");
		final var delegate = wrappableLevel(history);
		if (delegate.isEmpty()) {
			LOG.warn("History level '{}' cannot be wrapped - {} will keep being written to history", history, suppressedVariables);
			return;
		}

		final var level = new SensitiveVariableHistoryLevel(delegate.get(), suppressedVariables);
		final var levels = new ArrayList<>(ofNullable(configuration.getCustomHistoryLevels()).orElseGet(List::of));
		levels.add(level);
		configuration.setCustomHistoryLevels(levels);
		configuration.setHistory(SensitiveVariableHistoryLevel.NAME);

		LOG.info("History level '{}' wrapped as '{}' (id {}) - no history will be written for {}",
			history, level.getName(), level.getId(), suppressedVariables);
	}

	private static Optional<HistoryLevel> wrappableLevel(final String history) {
		return ofNullable(WRAPPABLE_LEVELS.get(history.toLowerCase()));
	}
}
