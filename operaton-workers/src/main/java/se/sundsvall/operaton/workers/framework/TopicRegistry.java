package se.sundsvall.operaton.workers.framework;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import se.sundsvall.operaton.workers.api.model.TopicDescription;
import se.sundsvall.operaton.workers.framework.annotation.TopicWorker;

/**
 * Scans all Spring beans for the @TopicWorker annotation and collects their metadata. This makes all registered
 * external task topics discoverable via the /topics endpoint. Uses {@link AnnotationUtils#findAnnotation} so the
 * annotation is also found on CGLIB-proxied beans (e.g. workers annotated with {@code @Dept44Scheduled}).
 */
@Component
public class TopicRegistry implements BeanPostProcessor {

	private final Map<String, TopicDescription> topics = new ConcurrentHashMap<>();

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		final var annotation = AnnotationUtils.findAnnotation(bean.getClass(), TopicWorker.class);
		if (annotation != null) {
			topics.put(annotation.topic(), TopicDescription.create()
				.withTopic(annotation.topic())
				.withDescription(annotation.description())
				.withInputVariables(List.of(annotation.inputVariables()))
				.withOutputVariables(List.of(annotation.outputVariables())));
		}
		return bean;
	}

	public List<TopicDescription> getAll() {
		return List.copyOf(topics.values());
	}

	public Optional<TopicDescription> getByTopic(final String topic) {
		return Optional.ofNullable(topics.get(topic));
	}
}
