package se.sundsvall.operaton.app.configuration;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.history.HistoryLevel;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveVariableHistoryPluginTest {

	private static final List<String> SUPPRESSED = List.of("financialAidBasis", "coApplicantFinancialAidBasis");

	private static StandaloneProcessEngineConfiguration configuration(final String history) {
		final var configuration = new StandaloneProcessEngineConfiguration();
		configuration.setHistory(history);
		return configuration;
	}

	@Test
	void registersTheLevelAndSelectsItByName() {
		final var configuration = configuration("full");

		new SensitiveVariableHistoryPlugin(SUPPRESSED).preInit(configuration);

		assertThat(configuration.getHistory()).isEqualTo("full-without-sensitive-variables");
		assertThat(configuration.getCustomHistoryLevels()).singleElement()
			.isInstanceOfSatisfying(SensitiveVariableHistoryLevel.class, level -> {
				assertThat(level.getDelegate()).isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL);
				assertThat(level.getSuppressedVariables()).containsExactlyInAnyOrderElementsOf(SUPPRESSED);
			});
	}

	/**
	 * The id has to come from the level actually in force, whatever that is, because the engine matches it against the
	 * one stored in ACT_GE_PROPERTY and will not start on a disagreement. Pinning 'full' here would have turned a
	 * deployment configured for audit into a engine that refuses to boot.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"none", "activity", "audit", "full"
	})
	void takesItsIdFromWhicheverLevelWasConfigured(final String history) {
		final var configuration = configuration(history);

		new SensitiveVariableHistoryPlugin(SUPPRESSED).preInit(configuration);

		final var level = (SensitiveVariableHistoryLevel) configuration.getCustomHistoryLevels().getFirst();
		assertThat(level.getId()).isEqualTo(level.getDelegate().getId());
		assertThat(level.getDelegate().getName()).isEqualTo(history);
	}

	/**
	 * 'auto' has no level to wrap yet — the engine resolves it from the database after preInit — and an unknown value
	 * is not ours to reinterpret. Both leave the engine exactly as configured, which means the payloads keep being
	 * written; the plugin logs a warning rather than failing, but this pins that it changes nothing.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"auto", "something-else"
	})
	void leavesTheEngineAloneWhenTheConfiguredLevelCannotBeWrapped(final String history) {
		final var configuration = configuration(history);

		new SensitiveVariableHistoryPlugin(SUPPRESSED).preInit(configuration);

		assertThat(configuration.getHistory()).isEqualTo(history);
		assertThat(configuration.getCustomHistoryLevels()).isNullOrEmpty();
	}

	@Test
	void leavesTheEngineAloneWhenNothingIsConfiguredForSuppression() {
		final var configuration = configuration("full");

		new SensitiveVariableHistoryPlugin(List.of(" ")).preInit(configuration);

		assertThat(configuration.getHistory()).isEqualTo("full");
		assertThat(configuration.getCustomHistoryLevels()).isNullOrEmpty();
	}

	/** Another plugin's custom level must survive ours being added. */
	@Test
	void keepsCustomLevelsRegisteredByAnyoneElse() {
		final var configuration = configuration("full");
		configuration.setCustomHistoryLevels(new ArrayList<>(List.of(HistoryLevel.HISTORY_LEVEL_AUDIT)));

		new SensitiveVariableHistoryPlugin(SUPPRESSED).preInit(configuration);

		assertThat(configuration.getCustomHistoryLevels())
			.hasSize(2)
			.element(0).isEqualTo(HistoryLevel.HISTORY_LEVEL_AUDIT);
	}
}
