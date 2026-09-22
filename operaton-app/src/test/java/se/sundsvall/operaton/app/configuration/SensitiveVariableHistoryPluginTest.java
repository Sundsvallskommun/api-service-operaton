package se.sundsvall.operaton.app.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.history.HistoryLevel;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveVariableHistoryPluginTest {

	private static final List<String> SUPPRESSED = List.of("financialAidBasis", "coApplicantFinancialAidBasis");

	/** The engine hands postInit a configuration whose history level is already resolved and validated. */
	private static StandaloneProcessEngineConfiguration resolvedAs(final HistoryLevel level) {
		final var configuration = new StandaloneProcessEngineConfiguration();
		configuration.setHistoryLevel(level);
		return configuration;
	}

	@Test
	void wrapsTheResolvedLevel() {
		final var configuration = resolvedAs(HistoryLevel.HISTORY_LEVEL_FULL);

		new SensitiveVariableHistoryPlugin(SUPPRESSED).postInit(configuration);

		assertThat(configuration.getHistoryLevel())
			.isInstanceOfSatisfying(SensitiveVariableHistoryLevel.class, level -> {
				assertThat(level.getDelegate()).isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL);
				assertThat(level.getSuppressedVariables()).containsExactlyInAnyOrderElementsOf(SUPPRESSED);
			});
	}

	/**
	 * The regression this class exists for. Every deployment that leaves {@code history} at its default resolves it
	 * from the database during init, and the previous version of this plugin matched the configured <em>string</em>
	 * against a table of level names at preInit - where the string is still {@code auto}. It therefore wrapped nothing,
	 * on exactly the deployments nobody had configured explicitly, while the configuration looked correct.
	 * <p>
	 * Reading the resolved level instead means the configured string never enters into it.
	 */
	@Test
	void wrapsEvenWhenHistoryWasLeftOnAuto() {
		final var configuration = resolvedAs(HistoryLevel.HISTORY_LEVEL_FULL);
		configuration.setHistory("auto");

		new SensitiveVariableHistoryPlugin(SUPPRESSED).postInit(configuration);

		assertThat(configuration.getHistoryLevel()).isInstanceOf(SensitiveVariableHistoryLevel.class);
	}

	/**
	 * The id has to come from the level actually in force, because the engine matches it against the one stored in
	 * ACT_GE_PROPERTY and will not start on a disagreement. Pinning one level here would turn a deployment configured
	 * for another into an engine that refuses to boot.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"none", "activity", "audit", "full"
	})
	void takesItsIdFromWhicheverLevelWasResolved(final String name) {
		final var resolved = levelNamed(name);
		final var configuration = resolvedAs(resolved);

		new SensitiveVariableHistoryPlugin(SUPPRESSED).postInit(configuration);

		final var level = (SensitiveVariableHistoryLevel) configuration.getHistoryLevel();
		assertThat(level.getId()).isEqualTo(resolved.getId());
		assertThat(level.getDelegate().getName()).isEqualTo(name);
	}

	@Test
	void leavesTheLevelAloneWithNothingToSuppress() {
		final var configuration = resolvedAs(HistoryLevel.HISTORY_LEVEL_FULL);

		new SensitiveVariableHistoryPlugin(List.of(" ")).postInit(configuration);

		assertThat(configuration.getHistoryLevel()).isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL);
	}

	@Test
	void doesNotWrapTwice() {
		final var configuration = resolvedAs(HistoryLevel.HISTORY_LEVEL_FULL);
		final var plugin = new SensitiveVariableHistoryPlugin(SUPPRESSED);

		plugin.postInit(configuration);
		final var afterFirst = configuration.getHistoryLevel();
		plugin.postInit(configuration);

		assertThat(configuration.getHistoryLevel()).isSameAs(afterFirst);
	}

	@Test
	void survivesAnUnresolvedLevel() {
		final var configuration = new StandaloneProcessEngineConfiguration();

		new SensitiveVariableHistoryPlugin(SUPPRESSED).postInit(configuration);

		assertThat(configuration.getHistoryLevel()).isNull();
	}

	private static HistoryLevel levelNamed(final String name) {
		return List.of(HistoryLevel.HISTORY_LEVEL_NONE, HistoryLevel.HISTORY_LEVEL_ACTIVITY,
			HistoryLevel.HISTORY_LEVEL_AUDIT, HistoryLevel.HISTORY_LEVEL_FULL).stream()
			.filter(level -> level.getName().equals(name))
			.findFirst()
			.orElseThrow();
	}
}
