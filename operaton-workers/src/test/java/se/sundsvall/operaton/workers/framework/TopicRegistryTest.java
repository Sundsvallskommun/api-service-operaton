package se.sundsvall.operaton.workers.framework;

import org.junit.jupiter.api.Test;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicRegistryTest {

	private final TopicRegistry topicRegistry = new TopicRegistry();

	@Test
	void testRegisterAndGetAll() {
		final var bean = new TestWorker();

		topicRegistry.postProcessAfterInitialization(bean, "testWorker");

		final var topics = topicRegistry.getAll();
		assertThat(topics).hasSize(1);
		assertThat(topics.getFirst().getTopic()).isEqualTo("test-topic");
		assertThat(topics.getFirst().getDescription()).isEqualTo("A test worker");
		assertThat(topics.getFirst().getInputVariables()).containsExactly("input1");
		assertThat(topics.getFirst().getOutputVariables()).containsExactly("output1");
	}

	@Test
	void testGetByTopic() {
		final var bean = new TestWorker();

		topicRegistry.postProcessAfterInitialization(bean, "testWorker");

		assertThat(topicRegistry.getByTopic("test-topic")).isPresent();
		assertThat(topicRegistry.getByTopic("non-existent")).isEmpty();
	}

	@Test
	void testNonAnnotatedBeanIsIgnored() {
		topicRegistry.postProcessAfterInitialization(new Object(), "plainBean");

		assertThat(topicRegistry.getAll()).isEmpty();
	}

	@Test
	void testAnnotationFoundOnSubclass() {
		// Simulates a CGLIB proxy: a subclass that inherits @TopicWorker from its parent class.
		// @TopicWorker is NOT @Inherited, so the registry must use AnnotationUtils.findAnnotation
		// to walk the class hierarchy rather than Class.getAnnotation.
		final var bean = new SubclassedWorker();

		topicRegistry.postProcessAfterInitialization(bean, "subclassedWorker");

		assertThat(topicRegistry.getByTopic("test-topic")).isPresent();
	}

	@Test
	void testDuplicateTopicFailsFast() {
		topicRegistry.postProcessAfterInitialization(new TestWorker(), "testWorker");

		assertThatThrownBy(() -> topicRegistry.postProcessAfterInitialization(new DuplicateTopicWorker(), "duplicateTopicWorker"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("test-topic")
			.hasMessageContaining(DuplicateTopicWorker.class.getName());
	}

	@TopicWorker(
		topic = "test-topic",
		description = "A test worker",
		inputVariables = {
			"input1"
		},
		outputVariables = {
			"output1"
		})
	private static class TestWorker {
	}

	private static class SubclassedWorker extends TestWorker {
	}

	@TopicWorker(
		topic = "test-topic",
		description = "Another worker declaring the same topic",
		inputVariables = {},
		outputVariables = {})
	private static class DuplicateTopicWorker {
	}
}
