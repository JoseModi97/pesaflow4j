package io.github.josemodi97.pesaflow4j.spring.boot3;

import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import org.springframework.context.ApplicationEvent;

/**
 * Published on the application event bus when the auto-registered webhook
 * controller (see {@link Pesaflow4jProperties.Webhook}) verifies a
 * successful notification. Listen for it instead of writing a controller:
 *
 * <pre>{@code
 * @EventListener
 * void onPaid(Pesaflow4jPaymentVerifiedEvent event) {
 *     orderService.markPaid(event.getResult().getReference(), event.getResult().getAmountPaid());
 * }
 * }</pre>
 */
public class Pesaflow4jPaymentVerifiedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final VerifyResult result;

    public Pesaflow4jPaymentVerifiedEvent(Object source, VerifyResult result) {
        super(source);
        this.result = result;
    }

    public VerifyResult getResult() {
        return result;
    }
}
