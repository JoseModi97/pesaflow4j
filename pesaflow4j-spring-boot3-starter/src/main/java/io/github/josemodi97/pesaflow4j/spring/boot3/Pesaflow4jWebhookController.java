package io.github.josemodi97.pesaflow4j.spring.boot3;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import java.util.Collections;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auto-registered when {@code pesaflow4j.webhook.enabled=true}: verifies an
 * inbound notification and republishes the outcome as a
 * {@link Pesaflow4jPaymentVerifiedEvent} / {@link Pesaflow4jPaymentRejectedEvent}
 * so application code can react with {@code @EventListener} instead of
 * writing its own controller.
 */
@RestController
public class Pesaflow4jWebhookController {

    private final Pesaflow4jClient client;
    private final ApplicationEventPublisher events;

    public Pesaflow4jWebhookController(Pesaflow4jClient client, ApplicationEventPublisher events) {
        this.client = client;
        this.events = events;
    }

    @PostMapping("${pesaflow4j.webhook.path:/pesaflow4j/notify}")
    public ResponseEntity<Map<String, String>> notify(@RequestParam Map<String, String> payload) {
        VerifyResult result = client.verify(payload);

        if (result.isSuccess()) {
            events.publishEvent(new Pesaflow4jPaymentVerifiedEvent(this, result));
            return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
        }

        events.publishEvent(new Pesaflow4jPaymentRejectedEvent(this, result));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("status", "error"));
    }
}
