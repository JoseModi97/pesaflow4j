package io.github.josemodi97.pesaflow4j;

import io.github.josemodi97.pesaflow4j.exception.Pesaflow4jConfigurationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable connection settings for a {@link Pesaflow4jClient}: merchant
 * credentials, the gateway endpoint, and defaults applied to every payment.
 *
 * <p>Build one with {@link #builder()}, or load it straight from environment
 * variables with {@link #fromEnvironment()} (useful for containers / CI where
 * credentials are injected rather than hardcoded).
 */
public final class Pesaflow4jConfig {

    public static final String DEFAULT_GATEWAY_URL = "https://payments.ecitizen.go.ke/PaymentAPI/iframev2.1.php";
    public static final String DEFAULT_CURRENCY = "KES";
    private static final Set<String> DEFAULT_SUCCESS_STATUSES = unmodifiableLowercaseSet(
            "paid", "settled", "success", "successful", "completed", "complete");

    private final String apiClientId;
    private final String apiKey;
    private final String secret;
    private final String serviceId;
    private final String gatewayUrl;
    private final String statusUrl;
    private final String pictureUrl;
    private final String currency;
    private final boolean sendStkPushByDefault;
    private final Set<String> successStatuses;
    private final int connectTimeoutMillis;

    private Pesaflow4jConfig(Builder builder) {
        this.apiClientId = trim(builder.apiClientId);
        this.apiKey = trim(builder.apiKey);
        this.secret = trim(builder.secret);
        this.serviceId = trim(builder.serviceId);
        this.gatewayUrl = builder.gatewayUrl == null || builder.gatewayUrl.trim().isEmpty()
                ? DEFAULT_GATEWAY_URL : builder.gatewayUrl.trim();
        this.statusUrl = trim(builder.statusUrl);
        this.pictureUrl = trim(builder.pictureUrl);
        this.currency = builder.currency == null || builder.currency.trim().isEmpty()
                ? DEFAULT_CURRENCY : builder.currency.trim();
        this.sendStkPushByDefault = builder.sendStkPushByDefault;
        this.successStatuses = builder.successStatuses == null
                ? DEFAULT_SUCCESS_STATUSES : unmodifiableLowercaseSet(builder.successStatuses.toArray(new String[0]));
        this.connectTimeoutMillis = builder.connectTimeoutMillis > 0 ? builder.connectTimeoutMillis : 30_000;
    }

    /**
     * Reads {@code PESAFLOW4J_CLIENT_ID}, {@code PESAFLOW4J_API_KEY},
     * {@code PESAFLOW4J_SECRET}, {@code PESAFLOW4J_SERVICE_ID},
     * {@code PESAFLOW4J_GATEWAY_URL}, {@code PESAFLOW4J_STATUS_URL} and
     * {@code PESAFLOW4J_CURRENCY} from the process environment.
     */
    public static Pesaflow4jConfig fromEnvironment() {
        return builder()
                .apiClientId(System.getenv("PESAFLOW4J_CLIENT_ID"))
                .apiKey(System.getenv("PESAFLOW4J_API_KEY"))
                .secret(System.getenv("PESAFLOW4J_SECRET"))
                .serviceId(System.getenv("PESAFLOW4J_SERVICE_ID"))
                .gatewayUrl(System.getenv("PESAFLOW4J_GATEWAY_URL"))
                .statusUrl(System.getenv("PESAFLOW4J_STATUS_URL"))
                .currency(System.getenv("PESAFLOW4J_CURRENCY"))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @throws Pesaflow4jConfigurationException if a required setting is blank */
    public void validate() {
        requireNonBlank("apiClientId", apiClientId);
        requireNonBlank("apiKey", apiKey);
        requireNonBlank("secret", secret);
        requireNonBlank("serviceId", serviceId);
        requireNonBlank("gatewayUrl", gatewayUrl);
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new Pesaflow4jConfigurationException(
                    "Pesaflow4jConfig is missing the required '" + name + "' setting. "
                            + "Set it via Pesaflow4jConfig.builder()." + name + "(...) or the matching "
                            + "PESAFLOW4J_* environment variable.");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static Set<String> unmodifiableLowercaseSet(String... values) {
        Set<String> set = new LinkedHashSet<>();
        for (String v : values) {
            if (v != null) {
                set.add(v.toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public String getApiClientId() {
        return apiClientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isSendStkPushByDefault() {
        return sendStkPushByDefault;
    }

    public Set<String> getSuccessStatuses() {
        return successStatuses;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public boolean isSuccessStatus(String status) {
        if (status == null) {
            return false;
        }
        return successStatuses.contains(status.trim().toLowerCase(Locale.ROOT));
    }

    /** Builder for {@link Pesaflow4jConfig}. */
    public static final class Builder {
        private String apiClientId;
        private String apiKey;
        private String secret;
        private String serviceId;
        private String gatewayUrl;
        private String statusUrl;
        private String pictureUrl;
        private String currency;
        private boolean sendStkPushByDefault;
        private Set<String> successStatuses;
        private int connectTimeoutMillis;

        private Builder() {
        }

        public Builder apiClientId(String apiClientId) {
            this.apiClientId = apiClientId;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder gatewayUrl(String gatewayUrl) {
            this.gatewayUrl = gatewayUrl;
            return this;
        }

        public Builder statusUrl(String statusUrl) {
            this.statusUrl = statusUrl;
            return this;
        }

        public Builder pictureUrl(String pictureUrl) {
            this.pictureUrl = pictureUrl;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder sendStkPushByDefault(boolean sendStkPushByDefault) {
            this.sendStkPushByDefault = sendStkPushByDefault;
            return this;
        }

        public Builder successStatuses(String... successStatuses) {
            this.successStatuses = new LinkedHashSet<>(Arrays.asList(successStatuses));
            return this;
        }

        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Pesaflow4jConfig build() {
            return new Pesaflow4jConfig(this);
        }
    }
}
