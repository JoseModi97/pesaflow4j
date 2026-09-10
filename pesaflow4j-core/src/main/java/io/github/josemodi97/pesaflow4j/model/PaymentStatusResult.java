package io.github.josemodi97.pesaflow4j.model;

/**
 * The raw HTTP response returned when polling the configured status endpoint
 * for the settlement status of a previously submitted invoice reference.
 */
public final class PaymentStatusResult {

    private final String requestUrl;
    private final int httpStatus;
    private final String responseBody;

    public PaymentStatusResult(String requestUrl, int httpStatus, String responseBody) {
        this.requestUrl = requestUrl;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
