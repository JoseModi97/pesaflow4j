package io.github.josemodi97.pesaflow4j.spring.boot3;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class Pesaflow4jAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Pesaflow4jAutoConfiguration.class));

    @Test
    void registersAClientBeanWhenCredentialsAreConfigured() {
        contextRunner
                .withPropertyValues(
                        "pesaflow4j.api-client-id=CID1",
                        "pesaflow4j.api-key=KEY1",
                        "pesaflow4j.secret=SECRET1",
                        "pesaflow4j.service-id=SID1")
                .run(context -> {
                    assertThat(context).hasSingleBean(Pesaflow4jClient.class);
                    Pesaflow4jClient client = context.getBean(Pesaflow4jClient.class);
                    assertThat(client.getGateway().getConfig().getApiClientId()).isEqualTo("CID1");
                });
    }

    @Test
    void doesNotRegisterAClientBeanWhenUnconfigured() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(Pesaflow4jClient.class));
    }

    @Test
    void bindsCurrencyAndWebhookProperties() {
        contextRunner
                .withPropertyValues(
                        "pesaflow4j.api-client-id=CID1",
                        "pesaflow4j.api-key=KEY1",
                        "pesaflow4j.secret=SECRET1",
                        "pesaflow4j.service-id=SID1",
                        "pesaflow4j.currency=USD",
                        "pesaflow4j.webhook.enabled=true",
                        "pesaflow4j.webhook.path=/custom/notify")
                .run(context -> {
                    Pesaflow4jProperties properties = context.getBean(Pesaflow4jProperties.class);
                    assertThat(properties.getCurrency()).isEqualTo("USD");
                    assertThat(properties.getWebhook().isEnabled()).isTrue();
                    assertThat(properties.getWebhook().getPath()).isEqualTo("/custom/notify");
                });
    }
}
