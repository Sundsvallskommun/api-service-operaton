package se.sundsvall.operaton.workers.messaging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import se.sundsvall.operaton.workers.messaging.configuration.MessagingProperties;

@AutoConfiguration
@ComponentScan("se.sundsvall.operaton.workers.messaging")
@EnableFeignClients(basePackageClasses = MessagingClient.class)
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingWorkersAutoConfiguration {
}
