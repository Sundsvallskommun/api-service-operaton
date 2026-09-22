package se.sundsvall.operaton.app.configuration;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.variable.serializer.StringValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.value.StringValue;

/**
 * The engine's own string serializer, with the length check it does not have.
 *
 * <p>
 * Operaton stores a string variable in {@code ACT_RU_VARIABLE.TEXT_} and {@code ACT_HI_DETAIL.TEXT_}, both
 * {@code varchar(4000)}, and {@link StringValueSerializer#writeValue} is a bare {@code setTextValue} — no bound, and no
 * spill to {@code ACT_GE_BYTEARRAY} the way {@code object}/{@code bytes}/{@code file} values get. A string one
 * character
 * too long therefore reaches MariaDB and comes back as {@code Data too long for column 'TEXT_'}, wrapped by the time it
 * surfaces into {@code "An exception occurred in the persistence layer"} — a message that names neither the variable
 * nor the length, on an incident that has already burned the whole retry ladder.
 *
 * <p>
 * That is exactly how the EB process went down on 2026-09-22 and stayed down for hours: the cause existed only in the
 * container log. This says it in the incident instead.
 *
 * <p>
 * The limit is not a tuning knob. A value that does not fit does not belong in the engine at all — it belongs in the
 * service that owns the data, with the process carrying a reference. Raising
 * {@code operaton.variables.max-string-length} past the column width only moves the failure back to MariaDB.
 */
public class BoundedStringValueSerializer extends StringValueSerializer {

	static final String TOO_LONG = """
		Process variable '%s' is %d characters; the engine stores string variables in a varchar(4000) column \
		(limit configured: %d). Do not widen the column — a value this size does not belong in the engine. \
		Keep it in the service that owns it and carry a reference in the process.""";

	private final int maxLength;

	public BoundedStringValueSerializer(final int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public void writeValue(final StringValue value, final ValueFields valueFields) {
		reject(value, valueFields);
		super.writeValue(value, valueFields);
	}

	/** The variable's name comes off {@link ValueFields}, which extends {@code Nameable} — so the failure can name it. */
	private void reject(final StringValue value, final ValueFields valueFields) {
		final var text = value.getValue();
		if (text == null) {
			return;
		}
		if (text.length() <= maxLength) {
			return;
		}
		throw new ProcessEngineException(TOO_LONG.formatted(valueFields.getName(), text.length(), maxLength));
	}
}
