package io.github.josemodi97.pesaflow4j.cli;

import io.github.josemodi97.pesaflow4j.Pesaflow4jConfig;
import picocli.CommandLine.Option;

/**
 * Shared connection flags for every subcommand. Any flag left unset falls
 * back to its {@code PESAFLOW4J_*} environment variable (see
 * {@link Pesaflow4jConfig#fromEnvironment()}), so CI/CD pipelines can omit
 * them entirely and rely on injected env vars.
 */
public final class CredentialsMixin {

    @Option(names = "--client-id", description = "Merchant API client ID (or PESAFLOW4J_CLIENT_ID)")
    String clientId;

    @Option(names = "--api-key", description = "Merchant API key (or PESAFLOW4J_API_KEY)")
    String apiKey;

    @Option(names = "--secret", description = "Merchant secret (or PESAFLOW4J_SECRET)")
    String secret;

    @Option(names = "--service-id", description = "Merchant service ID (or PESAFLOW4J_SERVICE_ID)")
    String serviceId;

    @Option(names = "--gateway-url", description = "Checkout endpoint override (or PESAFLOW4J_GATEWAY_URL)")
    String gatewayUrl;

    @Option(names = "--status-url", description = "Status polling endpoint (or PESAFLOW4J_STATUS_URL)")
    String statusUrl;

    @Option(names = "--currency", description = "ISO currency code (or PESAFLOW4J_CURRENCY, default KES)")
    String currency;

    public Pesaflow4jConfig toConfig() {
        Pesaflow4jConfig fromEnv = Pesaflow4jConfig.fromEnvironment();

        return Pesaflow4jConfig.builder()
                .apiClientId(firstNonBlank(clientId, fromEnv.getApiClientId()))
                .apiKey(firstNonBlank(apiKey, fromEnv.getApiKey()))
                .secret(firstNonBlank(secret, fromEnv.getSecret()))
                .serviceId(firstNonBlank(serviceId, fromEnv.getServiceId()))
                .gatewayUrl(firstNonBlank(gatewayUrl, fromEnv.getGatewayUrl()))
                .statusUrl(firstNonBlank(statusUrl, fromEnv.getStatusUrl()))
                .currency(firstNonBlank(currency, fromEnv.getCurrency()))
                .build();
    }

    private static String firstNonBlank(String flagValue, String envValue) {
        return (flagValue != null && !flagValue.trim().isEmpty()) ? flagValue : envValue;
    }
}
