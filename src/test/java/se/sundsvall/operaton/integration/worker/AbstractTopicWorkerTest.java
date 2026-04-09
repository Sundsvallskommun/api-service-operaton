package se.sundsvall.operaton.integration.worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static se.sundsvall.operaton.integration.worker.AbstractTopicWorker.optionalVariable;
import static se.sundsvall.operaton.integration.worker.AbstractTopicWorker.requireVariable;

@ExtendWith(MockitoExtension.class)
class AbstractTopicWorkerTest {

	@Mock
	private LockedExternalTask taskMock;

	@Test
	void requireVariableReturnsValueWhenPresentAndCorrectType() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("title", "Hello"));

		assertThat(requireVariable(taskMock, "title", String.class)).isEqualTo("Hello");
	}

	@Test
	void requireVariableThrowsWhenMissing() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables());

		assertThatThrownBy(() -> requireVariable(taskMock, "title", String.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Required process variable 'title' is missing on task task-1");
	}

	@Test
	void requireVariableThrowsWhenWrongType() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("title", 42));

		assertThatThrownBy(() -> requireVariable(taskMock, "title", String.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Process variable 'title' on task task-1 expected to be String but was Integer");
	}

	@Test
	void optionalVariableReturnsEmptyWhenMissing() {
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables());

		assertThat(optionalVariable(taskMock, "title", String.class)).isEmpty();
	}

	@Test
	void optionalVariableReturnsValueWhenPresentAndCorrectType() {
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("title", "Hello"));

		assertThat(optionalVariable(taskMock, "title", String.class)).contains("Hello");
	}

	@Test
	void optionalVariableThrowsWhenWrongType() {
		lenient().when(taskMock.getId()).thenReturn("task-1");
		lenient().when(taskMock.getVariables()).thenReturn(Variables.createVariables()
			.putValue("count", "not-an-integer"));

		assertThatThrownBy(() -> optionalVariable(taskMock, "count", Integer.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Process variable 'count' on task task-1 expected to be Integer but was String");
	}
}
