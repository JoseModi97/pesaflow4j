package io.github.josemodi97.pesaflow4j.spring.boot2;

import io.github.josemodi97.pesaflow4j.Pesaflow4jConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code pesaflow4j.*} entries in {@code application.yml}/{@code .properties}
 * to a {@link Pesaflow4jConfig}.
 *
 * <pre>{@code
 * pesaflow4j:
 *   api-client-id: ${PESAFLOW_CLIENT_ID}
 *   api-key: ${PESAFLOW_API_KEY}
 *   secret: ${PESAFLOW_SECRET}
 *   service-id: ${PESAFLOW_SERVICE_ID}
 *   currency: KES
 *   webhook:
 *     enabled: true
 *     path: /payments/notify
 * }</pre>
 */
@ConfigurationProperties(prefix = "pesaflow4j")
public class Pesaflow4jProperties {

    /** Merchant API client ID issued by the gateway. Required. */
    private String apiClientId;

    /** Merchant API key, used as the HMAC signing key. Required. */
    private String apiKey;

    /** Merchant secret, appended to every signed payload. Required. */
    private String secret;

    /** Merchant service ID. Required. */
    private String serviceId;

    /** Checkout endpoint. Defaults to the production PaymentAPI URL. */
    private String gatewayUrl;

    /** Settlement status polling endpoint. Optional. */
    private String statusUrl;

    /** Default billing picture/logo URL shown on the checkout page. Optional. */
    private String pictureUrl;

    /** ISO currency code. Defaults to {@code KES}. */
    private String currency;

    /** Whether to request an M-Pesa STK push by default when not set per-request. */
    private boolean sendStkPushByDefault;

    /** Statuses treated as a successful payment. Defaults to the standard set if empty. */
    private List<String> successStatuses = new ArrayList<>();

    /** Connect/read timeout in milliseconds for outbound gateway calls. */
    private int connectTimeoutMillis;

    private final Webhook webhook = new Webhook();

    public Pesaflow4jConfig toConfig() {
        Pesaflow4jConfig.Builder builder = Pesaflow4jConfig.builder()
                .apiClientId(apiClientId)
                .apiKey(apiKey)
                .secret(secret)
                .serviceId(serviceId)
                .sendStkPushByDefault(sendStkPushByDefault);

        if (gatewayUrl != null && !gatewayUrl.trim().isEmpty()) {
            builder.gatewayUrl(gatewayUrl);
        }
        if (statusUrl != null && !statusUrl.trim().isEmpty()) {
            builder.statusUrl(statusUrl);
        }
        if (pictureUrl != null && !pictureUrl.trim().isEmpty()) {
            builder.pictureUrl(pictureUrl);
        }
        if (currency != null && !currency.trim().isEmpty()) {
            builder.currency(currency);
        }
        if (!successStatuses.isEmpty()) {
            builder.successStatuses(successStatuses.toArray(new String[0]));
        }
        if (connectTimeoutMillis > 0) {
            builder.connectTimeoutMillis(connectTimeoutMillis);
        }

        return builder.build();
    }

    public String getApiClientId() {
        return apiClientId;
    }

    public void setApiClientId(String apiClientId) {
        this.apiClientId = apiClientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isSendStkPushByDefault() {
        return sendStkPushByDefault;
    }

    public void setSendStkPushByDefault(boolean sendStkPushByDefault) {
        this.sendStkPushByDefault = sendStkPushByDefault;
    }

    public List<String> getSuccessStatuses() {
        return successStatuses;
    }

    public void setSuccessStatuses(List<String> successStatuses) {
        this.successStatuses = successStatuses;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    /** {@code pesaflow4j.webhook.*} — the optional auto-registered notification endpoint. */
    public static class Webhook {

        /** Auto-register a webhook endpoint at {@link #path}. Off by default. */
        private boolean enabled;

        /** Path the webhook endpoint listens on, when {@link #enabled}. */
        private String path = "/pesaflow4j/notify";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
