package se.sundsvall.operaton.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.operaton.api.model.TopicDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

	@Mock
	private TopicRegistry topicRegistryMock;

	@InjectMocks
	private TopicService topicService;

	@Test
	void getTopics() {
		final var topicDescription = TopicDescription.create().withTopic("test-topic");
		when(topicRegistryMock.getAll()).thenReturn(List.of(topicDescription));

		final var result = topicService.getTopics();

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getTopic()).isEqualTo("test-topic");
		verify(topicRegistryMock).getAll();
	}

	@Test
	void getTopic() {
		final var topicDescription = TopicDescription.create().withTopic("test-topic");
		when(topicRegistryMock.getByTopic("test-topic")).thenReturn(Optional.of(topicDescription));

		final var result = topicService.getTopic("test-topic");

		assertThat(result.getTopic()).isEqualTo("test-topic");
		verify(topicRegistryMock).getByTopic("test-topic");
	}

	@Test
	void getTopicNotFound() {
		when(topicRegistryMock.getByTopic("non-existent")).thenReturn(Optional.empty());

		final var exception = assertThrows(ThrowableProblem.class,
			() -> topicService.getTopic("non-existent"));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		verify(topicRegistryMock).getByTopic("non-existent");
	}
}
