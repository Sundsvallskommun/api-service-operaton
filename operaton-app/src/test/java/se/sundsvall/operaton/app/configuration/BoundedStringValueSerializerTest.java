package se.sundsvall.operaton.app.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoundedStringValueSerializerTest {

	private static final int LIMIT = 4000;

	@Mock
	private ValueFields valueFieldsMock;

	private final BoundedStringValueSerializer serializer = new BoundedStringValueSerializer(LIMIT);

	/**
	 * The whole point: the failure must name the variable and its length. The engine's own message — "An exception
	 * occurred in the persistence layer" — names neither, which is why the EB outage took hours to diagnose.
	 */
	@Test
	void namesTheVariableAndTheLengthWhenTheValueIsTooLong() {
		when(valueFieldsMock.getName()).thenReturn("financialAidBasis");

		assertThatThrownBy(() -> serializer.writeValue(Variables.stringValue("x".repeat(4484)), valueFieldsMock))
			.isInstanceOf(ProcessEngineException.class)
			.hasMessageContaining("financialAidBasis")
			.hasMessageContaining("4484 characters")
			.hasMessageContaining("varchar(4000)")
			.hasMessageContaining("does not belong in the engine");

		verify(valueFieldsMock).getName();
	}

	@Test
	void writesAValueExactlyAtTheLimit() {
		final var atLimit = "x".repeat(LIMIT);

		serializer.writeValue(Variables.stringValue(atLimit), valueFieldsMock);

		verify(valueFieldsMock).setTextValue(atLimit);
	}

	@Test
	void writesAnOrdinaryValueUnchanged() {
		serializer.writeValue(Variables.stringValue("2026-06"), valueFieldsMock);

		verify(valueFieldsMock).setTextValue("2026-06");
	}

	/** A null value is the engine clearing a variable, not an oversized one. */
	@Test
	void allowsANullValue() {
		serializer.writeValue(Variables.stringValue(null), valueFieldsMock);

		verify(valueFieldsMock).setTextValue(null);
	}

	/** It is the engine's own string serializer with a bound added — reading must be untouched. */
	@Test
	void readsBackWhatTheEngineWouldRead() {
		when(valueFieldsMock.getTextValue()).thenReturn("stored");

		assertThat(serializer.readValue(valueFieldsMock, true).getValue()).isEqualTo("stored");
		assertThat(serializer.getName()).isEqualTo(Variables.stringValue("x").getType().getName());
	}

	/** The limit is configurable, so a deployment on a differently-sized column can say so. */
	@Test
	void honoursAConfiguredLimit() {
		when(valueFieldsMock.getName()).thenReturn("classifiedIncomes");

		assertThatThrownBy(() -> new BoundedStringValueSerializer(10).writeValue(Variables.stringValue("x".repeat(11)), valueFieldsMock))
			.isInstanceOf(ProcessEngineException.class)
			.hasMessageContaining("classifiedIncomes")
			.hasMessageContaining("11 characters")
			.hasMessageContaining("limit configured: 10");
	}
}
