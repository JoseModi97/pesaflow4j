package io.github.josemodi97.pesaflow4j.model;

import java.util.Collections;
import java.util.Map;

/**
 * The raw HTTP response returned after {@code Pesaflow4jClient.initiatePayment(...)}
 * submits a signed checkout payload directly to the gateway (no browser involved).
 */
public final class PaymentSubmissionResult {

    private final String requestUrl;
    private final Map<String, String> requestPayload;
    private final int httpStatus;
    private final String responseBody;

    public PaymentSubmissionResult(String requestUrl, Map<String, String> requestPayload,
                                    int httpStatus, String responseBody) {
        this.requestUrl = requestUrl;
        this.requestPayload = Collections.unmodifiableMap(requestPayload);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public Map<String, String> getRequestPayload() {
        return requestPayload;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
