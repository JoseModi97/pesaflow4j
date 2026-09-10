package io.github.josemodi97.pesaflow4j.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * The outcome of verifying an inbound webhook/callback notification against
 * the configured merchant secret.
 */
public final class VerifyResult {

    private final boolean success;
    private final boolean signatureValid;
    private final String status;
    private final String reference;
    private final BigDecimal amountPaid;
    private final Map<String, String> raw;
    private final String description;

    public VerifyResult(boolean success, boolean signatureValid, String status, String reference,
                         BigDecimal amountPaid, Map<String, String> raw, String description) {
        this.success = success;
        this.signatureValid = signatureValid;
        this.status = status;
        this.reference = reference;
        this.amountPaid = amountPaid;
        this.raw = Collections.unmodifiableMap(raw);
        this.description = description;
    }

    /** {@code true} only when the HMAC signature is valid AND the status is a configured success status. */
    public boolean isSuccess() {
        return success;
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public String getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    /** The raw, unmodified callback payload as received. */
    public Map<String, String> getRaw() {
        return raw;
    }

    public String getDescription() {
        return description;
    }
}
