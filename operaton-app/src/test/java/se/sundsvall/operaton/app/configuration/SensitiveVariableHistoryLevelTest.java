package se.sundsvall.operaton.app.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveVariableHistoryLevelTest {

	private static final String SUPPRESSED = "financialAidBasis";
	private static final String KEPT = "applicationMonth";

	private final SensitiveVariableHistoryLevel level = new SensitiveVariableHistoryLevel(
		HistoryLevel.HISTORY_LEVEL_FULL, List.of(SUPPRESSED, "coApplicantFinancialAidBasis"));

	private static VariableInstanceEntity variable(final String name) {
		final var entity = new VariableInstanceEntity();
		entity.setName(name);
		return entity;
	}

	/**
	 * The engine stores the active level's id in ACT_GE_PROPERTY and refuses to start when it disagrees with the
	 * configured one. Reporting the delegate's id is what makes this a substitution instead of a new level, so an
	 * engine that already holds data keeps starting.
	 */
	@Test
	void reportsTheWrappedLevelsIdSoAnExistingEngineStillStarts() {
		assertThat(level.getId()).isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL.getId());
		assertThat(level.getName()).isEqualTo("full-without-sensitive-variables")
			.isNotEqualTo(HistoryLevel.HISTORY_LEVEL_FULL.getName());
	}

	/**
	 * Every event type the variable history listener raises, for a suppressed variable. Letting any single one through
	 * would put the payload back in history — a create alone is the whole document.
	 */
	@ParameterizedTest
	@EnumSource(value = HistoryEventTypes.class, names = {
		"VARIABLE_INSTANCE_CREATE", "VARIABLE_INSTANCE_UPDATE", "VARIABLE_INSTANCE_MIGRATE",
		"VARIABLE_INSTANCE_UPDATE_DETAIL", "VARIABLE_INSTANCE_DELETE"
	})
	void producesNoEventOfAnyKindForASuppressedVariable(final HistoryEventTypes eventType) {
		assertThat(level.isHistoryEventProduced(eventType, variable(SUPPRESSED))).isFalse();
		// the same event for a variable nobody asked to suppress is the delegate's call, and full says yes
		assertThat(level.isHistoryEventProduced(eventType, variable(KEPT))).isTrue();
	}

	/**
	 * The listener hands the VariableInstanceEntity itself to isHistoryEventProduced — verified against the 2.1.4
	 * bytecode of VariableInstanceHistoryListener. If a future engine passed something else, the name would be
	 * unreadable and this level would silently stop suppressing, so the type is pinned here.
	 */
	@Test
	void readsTheVariableNameOffTheEntityTheListenerPasses() {
		final var entity = variable(SUPPRESSED);

		assertThat(entity.getName()).isEqualTo(SUPPRESSED);
		assertThat(level.isHistoryEventProduced(HistoryEventTypes.VARIABLE_INSTANCE_UPDATE, entity)).isFalse();
	}

	@Test
	void defersToTheDelegateForEverythingThatIsNotAVariableEvent() {
		assertThat(level.isHistoryEventProduced(HistoryEventTypes.ACTIVITY_INSTANCE_START, variable(SUPPRESSED)))
			.isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL.isHistoryEventProduced(HistoryEventTypes.ACTIVITY_INSTANCE_START, variable(SUPPRESSED)));
		assertThat(level.isHistoryEventProduced(HistoryEventTypes.PROCESS_INSTANCE_START, new ExecutionEntity()))
			.isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL.isHistoryEventProduced(HistoryEventTypes.PROCESS_INSTANCE_START, new ExecutionEntity()));
	}

	/** A variable event whose entity is not a variable is not ours to judge — it goes to the delegate untouched. */
	@Test
	void defersWhenTheEntityIsNotAVariable() {
		assertThat(level.isHistoryEventProduced(HistoryEventTypes.VARIABLE_INSTANCE_UPDATE, new ExecutionEntity()))
			.isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL.isHistoryEventProduced(HistoryEventTypes.VARIABLE_INSTANCE_UPDATE, new ExecutionEntity()));
	}

	/**
	 * Wrapping a quieter level must not make it louder: the suppression only ever removes history, never adds it.
	 */
	@Test
	void neverProducesMoreHistoryThanTheLevelItWraps() {
		final var overActivity = new SensitiveVariableHistoryLevel(HistoryLevel.HISTORY_LEVEL_ACTIVITY, List.of(SUPPRESSED));

		assertThat(overActivity.getId()).isEqualTo(HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId());
		assertThat(overActivity.isHistoryEventProduced(HistoryEventTypes.VARIABLE_INSTANCE_UPDATE, variable(KEPT)))
			.isEqualTo(HistoryLevel.HISTORY_LEVEL_ACTIVITY.isHistoryEventProduced(HistoryEventTypes.VARIABLE_INSTANCE_UPDATE, variable(KEPT)));
	}
}
