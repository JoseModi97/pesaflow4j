package io.github.josemodi97.pesaflow4j.model;

import java.util.Collections;
import java.util.Map;

/**
 * A signed checkout payload, ready to POST (or auto-submit via
 * {@code Pesaflow4jClient.payButton(...)}) to {@link #getUrl()}.
 */
public final class CheckoutResult {

    private final String url;
    private final Map<String, String> payload;

    public CheckoutResult(String url, Map<String, String> payload) {
        this.url = url;
        this.payload = Collections.unmodifiableMap(payload);
    }

    public String getUrl() {
        return url;
    }

    /** The signed form fields, in submission order, including {@code secureHash}. */
    public Map<String, String> getPayload() {
        return payload;
    }
}
