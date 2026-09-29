package se.sundsvall.operaton.app.configuration;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.operaton.bpm.engine.impl.history.AbstractHistoryLevel;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;

/**
 * A history level that behaves exactly like the one it wraps, except that it produces no history at all for a named set
 * of process variables. The engine's own {@code VariableInstanceHistoryListener} asks
 * {@link #isHistoryEventProduced(HistoryEventType, Object)} with the {@link VariableInstanceEntity} itself, so the
 * decision can be made on the variable's name.
 *
 * <p>
 * It exists for the SSBTEK basis. {@code financialAidBasis} is a ~4 kB JSON document carrying a person's personnummer,
 * agency income, vehicle ownership and residence-permit decisions, and every write of it was landing in
 * {@code ACT_HI_DETAIL} — a table nothing prunes and gallring never reaches. That is the same exposure
 * {@link PayloadLoggerSilencer} exists to prevent in the logs, with a longer life. It also broke the process outright:
 * {@code ACT_HI_DETAIL.TEXT_} is {@code varchar(4000)}, the payload grew past it, and every {@code fetch_ssbtek}
 * completion died with {@code Data too long for column 'TEXT_'} — the runtime write succeeded, the history insert
 * failed, and the shared transaction rolled the task back.
 *
 * <p>
 * The runtime variable is untouched, so downstream tasks read it exactly as before. That matters: a transient variable
 * would have solved the column problem too, but transient variables never leave their own transaction and
 * {@code EvaluateIncomeRulesWorker} reads this one from a later one.
 *
 * <p>
 * {@link #getId()} deliberately reports the wrapped level's id. The engine stores the active level's <em>id</em> in
 * {@code ACT_GE_PROPERTY} and refuses to start when it disagrees with the configured one, and there is no
 * {@code skipHistoryLevelCheck} in Operaton 2.1.4. Reporting the delegate's id makes this a substitution rather than a
 * new level, so an engine with data keeps starting and no property row has to be edited by hand.
 */
public class SensitiveVariableHistoryLevel extends AbstractHistoryLevel {

	/** The name the engine selects this level by — see {@code ProcessEngineConfigurationImpl.initHistoryLevel()}. */
	static final String NAME = "full-without-sensitive-variables";

	/** Every event type the variable history listener can raise; a suppressed variable produces none of them. */
	private static final Set<HistoryEventTypes> VARIABLE_EVENTS = EnumSet.of(
		HistoryEventTypes.VARIABLE_INSTANCE_CREATE,
		HistoryEventTypes.VARIABLE_INSTANCE_UPDATE,
		HistoryEventTypes.VARIABLE_INSTANCE_MIGRATE,
		HistoryEventTypes.VARIABLE_INSTANCE_UPDATE_DETAIL,
		HistoryEventTypes.VARIABLE_INSTANCE_DELETE);

	private final HistoryLevel delegate;
	private final Set<String> suppressedVariables;

	public SensitiveVariableHistoryLevel(final HistoryLevel delegate, final Collection<String> suppressedVariables) {
		this.delegate = delegate;
		this.suppressedVariables = Set.copyOf(suppressedVariables);
	}

	@Override
	public int getId() {
		return delegate.getId();
	}

	@Override
	public String getName() {
		return NAME;
	}

	/** The wrapped level's decision, except for a variable event naming one of the suppressed variables. */
	@Override
	public boolean isHistoryEventProduced(final HistoryEventType eventType, final Object entity) {
		if (isSuppressedVariableEvent(eventType, entity)) {
			return false;
		}
		return delegate.isHistoryEventProduced(eventType, entity);
	}

	/** The level this one defers to for everything it does not suppress. */
	HistoryLevel getDelegate() {
		return delegate;
	}

	Set<String> getSuppressedVariables() {
		return suppressedVariables;
	}

	private boolean isSuppressedVariableEvent(final HistoryEventType eventType, final Object entity) {
		if (!VARIABLE_EVENTS.contains(eventType)) {
			return false;
		}
		if (!(entity instanceof final VariableInstanceEntity variable)) {
			return false;
		}
		return suppressedVariables.contains(variable.getName());
	}
}
