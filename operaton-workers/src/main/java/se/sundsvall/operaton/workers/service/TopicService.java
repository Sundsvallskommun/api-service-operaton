package se.sundsvall.operaton.workers.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.workers.api.model.ElementTemplate;
import se.sundsvall.operaton.workers.api.model.TopicDescription;
import se.sundsvall.operaton.workers.framework.TopicRegistry;
import se.sundsvall.operaton.workers.service.mapper.ElementTemplateMapper;

@Service
public class TopicService {

	private static final String TOPIC_NOT_FOUND = "Topic '%s' not found";

	private final TopicRegistry topicRegistry;

	public TopicService(final TopicRegistry topicRegistry) {
		this.topicRegistry = topicRegistry;
	}

	public List<TopicDescription> getTopics() {
		return topicRegistry.getAll();
	}

	public TopicDescription getTopic(final String topic) {
		return topicRegistry.getByTopic(topic)
			.orElseThrow(() -> Problem.notFound(TOPIC_NOT_FOUND.formatted(topic)));
	}

	public List<ElementTemplate> getElementTemplates() {
		return topicRegistry.getAll().stream()
			.map(ElementTemplateMapper::toElementTemplate)
			.toList();
	}
}
