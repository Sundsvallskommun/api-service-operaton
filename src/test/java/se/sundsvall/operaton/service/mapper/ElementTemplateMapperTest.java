package se.sundsvall.operaton.service.mapper;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.operaton.api.model.TopicDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.APPLIES_TO_SERVICE_TASK;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.BINDING_NAME_TOPIC;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.BINDING_TYPE_INPUT_PARAMETER;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.BINDING_TYPE_PROPERTY;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.ID_PREFIX;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.PROPERTY_TYPE_STRING;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.SCHEMA_URL;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.TOPIC_PROPERTY_LABEL;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.humanize;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.toElementTemplate;
import static se.sundsvall.operaton.service.mapper.ElementTemplateMapper.toPascalCase;

class ElementTemplateMapperTest {

	@Test
	void privateConstructor() throws NoSuchMethodException {
		final var constructor = ElementTemplateMapper.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		assertThatNoException().isThrownBy(constructor::newInstance);
	}

	@Test
	void toElementTemplateWithNull() {
		assertThat(toElementTemplate(null)).isNull();
	}

	@Test
	void toElementTemplateWithFullSendEmailTopic() {
		final var topic = TopicDescription.create()
			.withTopic("send-email")
			.withDescription("Sends an email via the Messaging API")
			.withInputVariables(List.of("municipalityId", "emailAddress", "subject", "message", "senderName", "senderAddress"))
			.withOutputVariables(List.of("messageId"));

		final var result = toElementTemplate(topic);

		assertThat(result).isNotNull();
		assertThat(result.getSchema()).isEqualTo(SCHEMA_URL);
		assertThat(result.getId()).isEqualTo(ID_PREFIX + "SendEmail");
		assertThat(result.getName()).isEqualTo("Send Email");
		assertThat(result.getDescription()).isEqualTo("Sends an email via the Messaging API");
		assertThat(result.getAppliesTo()).containsExactly(APPLIES_TO_SERVICE_TASK);
		assertThat(result.getProperties()).hasSize(7);

		final var topicProperty = result.getProperties().getFirst();
		assertThat(topicProperty.getLabel()).isEqualTo(TOPIC_PROPERTY_LABEL);
		assertThat(topicProperty.getType()).isEqualTo(PROPERTY_TYPE_STRING);
		assertThat(topicProperty.getValue()).isEqualTo("send-email");
		assertThat(topicProperty.getEditable()).isFalse();
		assertThat(topicProperty.getBinding().getType()).isEqualTo(BINDING_TYPE_PROPERTY);
		assertThat(topicProperty.getBinding().getName()).isEqualTo(BINDING_NAME_TOPIC);
		assertThat(topicProperty.getConstraints()).isNull();

		assertThat(result.getProperties().subList(1, 7))
			.extracting("label")
			.containsExactly("Municipality Id", "Email Address", "Subject", "Message", "Sender Name", "Sender Address");
		assertThat(result.getProperties().subList(1, 7))
			.allSatisfy(p -> {
				assertThat(p.getType()).isEqualTo(PROPERTY_TYPE_STRING);
				assertThat(p.getValue()).isNull();
				assertThat(p.getEditable()).isNull();
				assertThat(p.getBinding().getType()).isEqualTo(BINDING_TYPE_INPUT_PARAMETER);
				assertThat(p.getConstraints().getNotEmpty()).isTrue();
			});
		assertThat(result.getProperties().subList(1, 7))
			.extracting(p -> p.getBinding().getName())
			.containsExactly("municipalityId", "emailAddress", "subject", "message", "senderName", "senderAddress");
	}

	@Test
	void toElementTemplateSkipsOutputVariables() {
		final var topic = TopicDescription.create()
			.withTopic("send-sms")
			.withDescription("Sends an sms")
			.withInputVariables(List.of("mobileNumber"))
			.withOutputVariables(List.of("messageId"));

		final var result = toElementTemplate(topic);

		assertThat(result).isNotNull();
		assertThat(result.getProperties()).hasSize(2);
		assertThat(result.getProperties().getFirst().getBinding().getName()).isEqualTo(BINDING_NAME_TOPIC);
		assertThat(result.getProperties().get(1).getBinding().getName()).isEqualTo("mobileNumber");
	}

	@Test
	void toElementTemplateWithNoInputVariables() {
		final var topic = TopicDescription.create()
			.withTopic("log-message")
			.withDescription("Logs a message");

		final var result = toElementTemplate(topic);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ID_PREFIX + "LogMessage");
		assertThat(result.getName()).isEqualTo("Log Message");
		assertThat(result.getProperties()).hasSize(1);
		assertThat(result.getProperties().getFirst().getLabel()).isEqualTo(TOPIC_PROPERTY_LABEL);
	}

	@Test
	void toElementTemplateWithEmptyInputVariables() {
		final var topic = TopicDescription.create()
			.withTopic("log-message")
			.withInputVariables(List.of());

		final var result = toElementTemplate(topic);

		assertThat(result.getProperties()).hasSize(1);
	}

	@Test
	void humanizeKebabCase() {
		assertThat(humanize("send-email")).isEqualTo("Send Email");
		assertThat(humanize("create-errand")).isEqualTo("Create Errand");
	}

	@Test
	void humanizeCamelCase() {
		assertThat(humanize("emailAddress")).isEqualTo("Email Address");
		assertThat(humanize("municipalityId")).isEqualTo("Municipality Id");
		assertThat(humanize("subject")).isEqualTo("Subject");
	}

	@Test
	void humanizeMixedCase() {
		assertThat(humanize("log-message-now")).isEqualTo("Log Message Now");
		assertThat(humanize("sendEmailTo")).isEqualTo("Send Email To");
	}

	@Test
	void humanizeNullOrBlank() {
		assertThat(humanize(null)).isNull();
		assertThat(humanize("")).isNull();
		assertThat(humanize("   ")).isNull();
	}

	@Test
	void toPascalCaseKebab() {
		assertThat(toPascalCase("send-email")).isEqualTo("SendEmail");
		assertThat(toPascalCase("create-errand")).isEqualTo("CreateErrand");
		assertThat(toPascalCase("log-message")).isEqualTo("LogMessage");
	}

	@Test
	void toPascalCaseCamel() {
		assertThat(toPascalCase("logMessage")).isEqualTo("LogMessage");
		assertThat(toPascalCase("single")).isEqualTo("Single");
	}

	@Test
	void toPascalCaseNullOrBlank() {
		assertThat(toPascalCase(null)).isNull();
		assertThat(toPascalCase("")).isNull();
	}
}
