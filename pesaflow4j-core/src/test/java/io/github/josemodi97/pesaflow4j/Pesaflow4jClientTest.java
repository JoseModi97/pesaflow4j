package io.github.josemodi97.pesaflow4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.pesaflow4j.internal.HmacSigner;
import io.github.josemodi97.pesaflow4j.model.CheckoutRequest;
import io.github.josemodi97.pesaflow4j.model.CheckoutResult;
import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Pesaflow4jClientTest {

    private static final String API_KEY = "APIKEY1";
    private static final String SECRET = "SECRET1";

    private Pesaflow4jClient client() {
        return new Pesaflow4jClient(Pesaflow4jConfig.builder()
                .apiClientId("CID1")
                .apiKey(API_KEY)
                .secret(SECRET)
                .serviceId("SID1")
                .build());
    }

    /**
     * Signs a fake notification payload exactly as the real gateway would,
     * so these tests can exercise {@code verify()}'s status/signature
     * handling without depending on a live gateway.
     */
    private static String signNotification(Map<String, String> notification) {
        String data = notification.getOrDefault("client_invoice_ref", "")
                + notification.getOrDefault("invoice_number", "")
                + notification.getOrDefault("amount_paid", "")
                + notification.getOrDefault("payment_date", "")
                + SECRET;
        return HmacSigner.signHexThenBase64(data, API_KEY);
    }

    @Test
    void checkoutReturnsSignedPayloadAndConfiguredGatewayUrl() {
        CheckoutResult result = client().checkout(CheckoutRequest.builder()
                .amount(500).reference("INV-0001").description("School fees")
                .name("Jane Doe").idNumber("12345678").phone("0712345678").build());

        assertEquals(Pesaflow4jConfig.DEFAULT_GATEWAY_URL, result.getUrl());
        assertEquals("254712345678", result.getPayload().get("clientMSISDN"));
        assertFalse(result.getPayload().get("secureHash").isEmpty());
    }

    @Test
    void payButtonEscapesUntrustedFieldValues() {
        String html = client().payButton(CheckoutRequest.builder()
                .amount(500).reference("INV-0001").description("School fees")
                .name("<script>alert(1)</script>").idNumber("12345678").build());

        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>alert(1)</script>"));
    }

    @Test
    void verifyRoundTripsAGatewaySignedNotification() {
        Pesaflow4jClient client = client();

        // Build a checkout hash the same way the gateway would sign an outbound
        // notification, then feed it back through verify() as if it were an
        // inbound webhook call.
        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("invoice_number", "TXN123");
        notification.put("amount_paid", "500.00");
        notification.put("payment_date", "2026-01-15");
        notification.put("status", "settled");
        String hash = signNotification(notification);
        notification.put("secure_hash", hash);

        VerifyResult verified = client.verify(notification);

        assertTrue(verified.isSignatureValid());
        assertTrue(verified.isSuccess());
        assertEquals("INV-0001", verified.getReference());
        assertEquals(0, verified.getAmountPaid().compareTo(new java.math.BigDecimal("500.00")));
    }

    @Test
    void verifyFailsOnUnrecognizedStatusEvenWithValidSignature() {
        Pesaflow4jClient client = client();

        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("invoice_number", "TXN123");
        notification.put("amount_paid", "500.00");
        notification.put("payment_date", "2026-01-15");
        notification.put("status", "pending");
        String hash = signNotification(notification);
        notification.put("secure_hash", hash);

        VerifyResult verified = client.verify(notification);

        assertTrue(verified.isSignatureValid());
        assertFalse(verified.isSuccess());
    }

    @Test
    void isPaidIsShorthandForVerifySuccess() {
        Pesaflow4jClient client = client();
        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("amount_paid", "500.00");
        notification.put("status", "paid");
        String hash = signNotification(notification);
        notification.put("secure_hash", hash);

        assertTrue(client.isPaid(notification));
    }
}
