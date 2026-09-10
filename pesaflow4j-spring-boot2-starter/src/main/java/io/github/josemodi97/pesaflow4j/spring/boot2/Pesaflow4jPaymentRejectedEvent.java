package io.github.josemodi97.pesaflow4j.spring.boot2;

import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import org.springframework.context.ApplicationEvent;

/**
 * Published on the application event bus when the auto-registered webhook
 * controller receives a notification that fails signature or status
 * verification (e.g. tampered payload, or a non-success status).
 */
public class Pesaflow4jPaymentRejectedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final VerifyResult result;

    public Pesaflow4jPaymentRejectedEvent(Object source, VerifyResult result) {
        super(source);
        this.result = result;
    }

    public VerifyResult getResult() {
        return result;
    }
}
