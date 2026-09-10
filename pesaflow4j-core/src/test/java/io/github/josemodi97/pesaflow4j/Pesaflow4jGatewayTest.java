package io.github.josemodi97.pesaflow4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.pesaflow4j.exception.Pesaflow4jValidationException;
import io.github.josemodi97.pesaflow4j.model.CheckoutRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Golden-vector tests: the expected HMAC values were computed independently
 * with a throwaway JDK program (not this library's own code) so a bug in
 * {@code Pesaflow4jGateway} can't silently self-validate.
 */
class Pesaflow4jGatewayTest {

    private static final String GOLDEN_CHECKOUT_HASH =
            "MzY5Mzk0YmI5NzE5NTIxNWE0N2FjZWRmMWJjNzQ0MTk2OWFlNTkyYzM4N2ExMzUwOTMyYmU4ZDE1YTlhZjA1Mg==";
    private static final String GOLDEN_NOTIFY_HASH =
            "NGMxZmQxOTg1MDc3MGNiYzI3MWFiZmU4NWM2N2Y1MjBjNWUxYTY1ZWEwYmMyODVkZWU3ODc1NDY4MzRkMWRhNA==";

    private Pesaflow4jConfig testConfig() {
        return Pesaflow4jConfig.builder()
                .apiClientId("CID1")
                .apiKey("APIKEY1")
                .secret("SECRET1")
                .serviceId("SID1")
                .build();
    }

    @Test
    void generatesCheckoutHashMatchingKnownVector() {
        Pesaflow4jGateway gateway = new Pesaflow4jGateway(testConfig());

        CheckoutRequest request = CheckoutRequest.builder()
                .amount(500)
                .reference("INV-0001")
                .description("School fees")
                .name("Jane Doe")
                .idNumber("12345678")
                .currency("KES")
                .build();

        Map<String, String> payload = gateway.buildCheckoutPayload(request);

        assertEquals("500.00", payload.get("amountExpected"));
        assertEquals(GOLDEN_CHECKOUT_HASH, payload.get("secureHash"));
    }

    @Test
    void checkoutHashChangesWhenAnyFieldChanges() {
        Pesaflow4jGateway gateway = new Pesaflow4jGateway(testConfig());

        CheckoutRequest base = CheckoutRequest.builder()
                .amount(500).reference("INV-0001").description("School fees")
                .name("Jane Doe").idNumber("12345678").build();
        CheckoutRequest mutated = CheckoutRequest.builder()
                .amount(501).reference("INV-0001").description("School fees")
                .name("Jane Doe").idNumber("12345678").build();

        String baseHash = gateway.buildCheckoutPayload(base).get("secureHash");
        String mutatedHash = gateway.buildCheckoutPayload(mutated).get("secureHash");

        assertNotEquals(baseHash, mutatedHash);
    }

    @Test
    void verifiesNotificationHashMatchingKnownVector() {
        Pesaflow4jGateway gateway = new Pesaflow4jGateway(testConfig());

        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("invoice_number", "TXN123");
        notification.put("amount_paid", "500.00");
        notification.put("payment_date", "2026-01-15");
        notification.put("secure_hash", GOLDEN_NOTIFY_HASH);

        assertTrue(gateway.verifyNotificationHash(notification));
    }

    @Test
    void rejectsTamperedNotification() {
        Pesaflow4jGateway gateway = new Pesaflow4jGateway(testConfig());

        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("invoice_number", "TXN123");
        notification.put("amount_paid", "999999.00"); // tampered amount
        notification.put("payment_date", "2026-01-15");
        notification.put("secure_hash", GOLDEN_NOTIFY_HASH);

        assertFalse(gateway.verifyNotificationHash(notification));
    }

    @Test
    void rejectsMissingSignature() {
        Pesaflow4jGateway gateway = new Pesaflow4jGateway(testConfig());
        assertFalse(gateway.verifyNotificationHash(new LinkedHashMap<>()));
    }

    @Test
    void rejectsIncompleteCheckoutRequest() {
        Pesaflow4jGateway gateway = new Pesaflow4jGateway(testConfig());
        CheckoutRequest incomplete = CheckoutRequest.builder().amount(500).build();

        assertThrows(Pesaflow4jValidationException.class, () -> gateway.buildCheckoutPayload(incomplete));
    }
}
