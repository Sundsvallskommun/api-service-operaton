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
 *
 * <p>
 * Topics live in a flat, global namespace (both in the engine's fetch-and-lock and in this catalog), so two workers
 * declaring the same topic would otherwise silently overwrite each other here and be routed to non-deterministically.
 * To turn that latent bug into a loud one, a duplicate topic fails application startup.
 */
@Component
public class TopicRegistry implements BeanPostProcessor {

	private final Map<String, TopicDescription> topics = new ConcurrentHashMap<>();

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		final var annotation = AnnotationUtils.findAnnotation(bean.getClass(), TopicWorker.class);
		if (annotation != null) {
			final var existing = topics.putIfAbsent(annotation.topic(), TopicDescription.create()
				.withTopic(annotation.topic())
				.withDescription(annotation.description())
				.withInputVariables(List.of(annotation.inputVariables()))
				.withOutputVariables(List.of(annotation.outputVariables())));
			if (existing != null) {
				throw new IllegalStateException(
					"Duplicate @TopicWorker topic '%s' declared by %s — topics must be unique across all workers".formatted(
						annotation.topic(), bean.getClass().getName()));
			}
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
