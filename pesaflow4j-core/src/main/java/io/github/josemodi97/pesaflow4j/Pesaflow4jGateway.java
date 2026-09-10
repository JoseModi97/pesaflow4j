package io.github.josemodi97.pesaflow4j;

import io.github.josemodi97.pesaflow4j.internal.HmacSigner;
import io.github.josemodi97.pesaflow4j.model.CheckoutRequest;
import io.github.josemodi97.pesaflow4j.util.PhoneNormalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Low-level signing/verification engine: builds signed checkout payloads and
 * checks inbound notification signatures against a {@link Pesaflow4jConfig}.
 * Uses only {@code javax.crypto} &mdash; no third-party dependency.
 *
 * <p>Most applications should use {@link Pesaflow4jClient} instead; reach for
 * this class directly only if you need the raw payload/signature without the
 * HTTP submission or HTML rendering conveniences.
 */
public final class Pesaflow4jGateway {

    private final Pesaflow4jConfig config;

    public Pesaflow4jGateway(Pesaflow4jConfig config) {
        this.config = config;
        this.config.validate();
    }

    public Pesaflow4jConfig getConfig() {
        return config;
    }

    /**
     * Builds the signed field map (including {@code secureHash}) ready to be
     * submitted to {@link Pesaflow4jConfig#getGatewayUrl()}.
     */
    public Map<String, String> buildCheckoutPayload(CheckoutRequest request) {
        request.validate();

        String amount = normalizeAmount(request.getAmount());
        String phone = request.getPhone() == null ? "" : PhoneNormalizer.normalize(request.getPhone());
        boolean sendStk = request.getSendStkPush() != null ? request.getSendStkPush() : config.isSendStkPushByDefault();

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("apiClientID", config.getApiClientId());
        payload.put("serviceID", config.getServiceId());
        payload.put("billDesc", request.getDescription());
        payload.put("currency", request.getCurrency() != null ? request.getCurrency() : config.getCurrency());
        payload.put("billRefNumber", request.getReference());
        payload.put("clientMSISDN", phone);
        payload.put("clientName", request.getName());
        payload.put("clientIDNumber", request.getIdNumber());
        payload.put("clientEmail", request.getEmail() != null ? request.getEmail() : "");
        payload.put("callBackURLOnSuccess", request.getCallbackUrl() != null ? request.getCallbackUrl() : "");
        payload.put("amountExpected", amount);
        payload.put("notificationURL", request.getNotifyUrl() != null ? request.getNotifyUrl() : "");
        payload.put("pictureURL", request.getPictureUrl() != null ? request.getPictureUrl() : config.getPictureUrl());
        payload.put("format", request.getFormat() != null ? request.getFormat() : "iframe");

        if (sendStk) {
            payload.put("sendSTK", "true");
        }

        for (Map.Entry<String, String> entry : request.getExtra().entrySet()) {
            payload.put(entry.getKey(), entry.getValue());
        }

        payload.put("secureHash", generateCheckoutHash(payload));

        return payload;
    }

    /**
     * Signature order: {@code apiClientID + amountExpected + serviceID + clientIDNumber
     * + currency + billRefNumber + billDesc + clientName + secret}, HMAC-SHA256 keyed
     * by {@code apiKey}, hex-encoded then Base64-encoded.
     */
    public String generateCheckoutHash(Map<String, String> payload) {
        String data = config.getApiClientId()
                + nullToEmpty(payload.get("amountExpected"))
                + config.getServiceId()
                + nullToEmpty(payload.get("clientIDNumber"))
                + nullToEmpty(payload.get("currency"))
                + nullToEmpty(payload.get("billRefNumber"))
                + nullToEmpty(payload.get("billDesc"))
                + nullToEmpty(payload.get("clientName"))
                + config.getSecret();

        return HmacSigner.signHexThenBase64(data, config.getApiKey());
    }

    /**
     * Verifies the {@code secure_hash} / {@code secureHash} field on an inbound
     * callback or notification payload against the configured secret, using a
     * timing-safe comparison.
     */
    public boolean verifyNotificationHash(Map<String, String> payload) {
        String providedHash = firstNonBlank(payload.get("secure_hash"), payload.get("secureHash"));
        if (providedHash == null || providedHash.trim().isEmpty()) {
            return false;
        }

        String data = firstNonBlank(payload.get("client_invoice_ref"), payload.get("billRefNumber"), "")
                + nullToEmpty(payload.get("invoice_number"))
                + firstNonBlank(payload.get("amount_paid"), payload.get("amount"), "")
                + nullToEmpty(payload.get("payment_date"))
                + config.getSecret();

        String expectedHash = HmacSigner.signHexThenBase64(data, config.getApiKey());

        return HmacSigner.timingSafeEquals(expectedHash, providedHash.trim());
    }

    public boolean isSuccessStatus(String status) {
        return config.isSuccessStatus(status);
    }

    private static String normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.trim().isEmpty()) ? a : b;
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        String first = firstNonBlank(a, b);
        return (first != null && !first.trim().isEmpty()) ? first : fallback;
    }
}
