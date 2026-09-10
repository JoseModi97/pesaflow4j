package io.github.josemodi97.pesaflow4j.spring.boot3;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configures a {@link Pesaflow4jClient} bean from {@code pesaflow4j.*}
 * properties, and (opt-in) a webhook endpoint that republishes verified
 * notifications as application events.
 *
 * <p>Only activates once {@code pesaflow4j.api-client-id} is set, so an
 * unconfigured app (or a test slice that doesn't need it) doesn't fail to
 * start just because this starter is on the classpath.
 */
@Configuration
@EnableConfigurationProperties(Pesaflow4jProperties.class)
@ConditionalOnClass(Pesaflow4jClient.class)
@ConditionalOnProperty(prefix = "pesaflow4j", name = "api-client-id")
public class Pesaflow4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Pesaflow4jClient pesaflow4jClient(Pesaflow4jProperties properties) {
        return new Pesaflow4jClient(properties.toConfig());
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnProperty(prefix = "pesaflow4j.webhook", name = "enabled", havingValue = "true")
    static class WebhookConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Pesaflow4jWebhookController pesaflow4jWebhookController(
                Pesaflow4jClient client, ApplicationEventPublisher events) {
            return new Pesaflow4jWebhookController(client, events);
        }
    }
}
