package io.github.josemodi97.pesaflow4j.spring.boot3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.Pesaflow4jConfig;
import io.github.josemodi97.pesaflow4j.internal.HmacSigner;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class Pesaflow4jWebhookControllerTest {

    private static final String API_KEY = "APIKEY1";
    private static final String SECRET = "SECRET1";

    private Pesaflow4jClient client() {
        return new Pesaflow4jClient(Pesaflow4jConfig.builder()
                .apiClientId("CID1").apiKey(API_KEY).secret(SECRET).serviceId("SID1").build());
    }

    private static String sign(Map<String, String> notification) {
        String data = notification.getOrDefault("client_invoice_ref", "")
                + notification.getOrDefault("invoice_number", "")
                + notification.getOrDefault("amount_paid", "")
                + notification.getOrDefault("payment_date", "")
                + SECRET;
        return HmacSigner.signHexThenBase64(data, API_KEY);
    }

    @Test
    void publishesVerifiedEventAndReturnsOkForAValidNotification() {
        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("amount_paid", "500.00");
        notification.put("status", "settled");
        notification.put("secure_hash", sign(notification));

        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        Pesaflow4jWebhookController controller = new Pesaflow4jWebhookController(client(), events);

        ResponseEntity<Map<String, String>> response = controller.notify(notification);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(events).publishEvent(any(Pesaflow4jPaymentVerifiedEvent.class));
    }

    @Test
    void publishesRejectedEventAndReturnsBadRequestForATamperedNotification() {
        Map<String, String> notification = new LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("amount_paid", "500.00");
        notification.put("status", "settled");
        notification.put("secure_hash", "not-a-real-signature");

        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        Pesaflow4jWebhookController controller = new Pesaflow4jWebhookController(client(), events);

        ResponseEntity<Map<String, String>> response = controller.notify(notification);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(events).publishEvent(any(Pesaflow4jPaymentRejectedEvent.class));
    }
}
