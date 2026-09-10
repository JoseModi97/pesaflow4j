package io.github.josemodi97.pesaflow4j;

import io.github.josemodi97.pesaflow4j.internal.HtmlEscaper;
import io.github.josemodi97.pesaflow4j.internal.HttpTransport;
import io.github.josemodi97.pesaflow4j.model.CheckoutRequest;
import io.github.josemodi97.pesaflow4j.model.CheckoutResult;
import io.github.josemodi97.pesaflow4j.model.PayButtonOptions;
import io.github.josemodi97.pesaflow4j.model.PaymentStatusResult;
import io.github.josemodi97.pesaflow4j.model.PaymentSubmissionResult;
import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The main entry point of PesaFlow4J: build signed checkout payloads, render
 * pay buttons, submit payments headlessly, poll settlement status, and
 * verify inbound webhook notifications.
 *
 * <p>Thread-safe and cheap to hold as a singleton/bean for the lifetime of
 * the application, once constructed with a valid {@link Pesaflow4jConfig}.
 *
 * <pre>{@code
 * Pesaflow4jClient client = new Pesaflow4jClient(Pesaflow4jConfig.builder()
 *         .apiClientId("...")
 *         .apiKey("...")
 *         .secret("...")
 *         .serviceId("...")
 *         .build());
 *
 * CheckoutResult checkout = client.checkout(CheckoutRequest.builder()
 *         .amount(500)
 *         .reference("INV-0001")
 *         .description("School fees")
 *         .name("Jane Doe")
 *         .idNumber("12345678")
 *         .phone("0712345678")
 *         .build());
 * }</pre>
 */
public final class Pesaflow4jClient {

    private final Pesaflow4jGateway gateway;

    public Pesaflow4jClient(Pesaflow4jConfig config) {
        this.gateway = new Pesaflow4jGateway(config);
    }

    /** Builds and signs a checkout payload. POST {@link CheckoutResult#getPayload()} to {@link CheckoutResult#getUrl()}. */
    public CheckoutResult checkout(CheckoutRequest request) {
        Map<String, String> payload = gateway.buildCheckoutPayload(request);
        return new CheckoutResult(gateway.getConfig().getGatewayUrl(), payload);
    }

    /**
     * Renders a ready-to-use, self-contained HTML {@code <form>} with a submit
     * button that sends the payer to the gateway. No frontend JavaScript or
     * iframe wiring required.
     */
    public String payButton(CheckoutRequest request, String buttonLabel, PayButtonOptions options) {
        CheckoutResult checkout = checkout(request);
        PayButtonOptions opts = options != null ? options : PayButtonOptions.create();

        StringBuilder html = new StringBuilder();
        html.append("<form action=\"").append(HtmlEscaper.escapeAttribute(checkout.getUrl()))
                .append("\" method=\"post\" target=\"")
                .append(HtmlEscaper.escapeAttribute(opts.getTarget() != null ? opts.getTarget() : "_blank"))
                .append("\">");

        for (Map.Entry<String, String> field : checkout.getPayload().entrySet()) {
            html.append("<input type=\"hidden\" name=\"").append(HtmlEscaper.escapeAttribute(field.getKey()))
                    .append("\" value=\"").append(HtmlEscaper.escapeAttribute(field.getValue())).append("\">");
        }

        html.append("<button type=\"submit\"");
        if (opts.getCssClass() != null && !opts.getCssClass().isEmpty()) {
            html.append(" class=\"").append(HtmlEscaper.escapeAttribute(opts.getCssClass())).append("\"");
        }
        if (opts.getId() != null && !opts.getId().isEmpty()) {
            html.append(" id=\"").append(HtmlEscaper.escapeAttribute(opts.getId())).append("\"");
        }
        if (opts.getStyle() != null && !opts.getStyle().isEmpty()) {
            html.append(" style=\"").append(HtmlEscaper.escapeAttribute(opts.getStyle())).append("\"");
        }
        for (Map.Entry<String, String> attr : opts.getExtraAttributes().entrySet()) {
            html.append(" ").append(HtmlEscaper.escapeAttribute(attr.getKey()))
                    .append("=\"").append(HtmlEscaper.escapeAttribute(attr.getValue())).append("\"");
        }
        html.append(">").append(HtmlEscaper.escapeText(buttonLabel != null ? buttonLabel : "Pay Now")).append("</button>");
        html.append("</form>");

        return html.toString();
    }

    /** {@link #payButton(CheckoutRequest, String, PayButtonOptions)} with default label and styling. */
    public String payButton(CheckoutRequest request) {
        return payButton(request, "Pay Now", PayButtonOptions.create());
    }

    /**
     * Signs a checkout payload and submits it directly to the gateway from the
     * server &mdash; no browser, no HTML form. Use this to initiate a payment
     * (e.g. trigger an M-Pesa STK push) headlessly.
     */
    public PaymentSubmissionResult initiatePayment(CheckoutRequest request) {
        CheckoutResult checkout = checkout(request);
        HttpTransport.HttpResponse response = HttpTransport.postForm(
                checkout.getUrl(), checkout.getPayload(), gateway.getConfig().getConnectTimeoutMillis());
        return new PaymentSubmissionResult(response.requestUrl, checkout.getPayload(), response.httpStatus, response.body);
    }

    /** Asynchronous variant of {@link #initiatePayment(CheckoutRequest)}. */
    public CompletableFuture<PaymentSubmissionResult> initiatePaymentAsync(CheckoutRequest request) {
        return CompletableFuture.supplyAsync(() -> initiatePayment(request));
    }

    /**
     * Polls the configured status endpoint ({@link Pesaflow4jConfig#getStatusUrl()})
     * for the settlement status of a previously submitted invoice reference.
     */
    public PaymentStatusResult checkPaymentStatus(String reference, Map<String, String> extraParams) {
        String statusUrl = gateway.getConfig().getStatusUrl();
        if (statusUrl == null || statusUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                    "checkPaymentStatus() requires a status URL. Set Pesaflow4jConfig.builder().statusUrl(\"...\") "
                            + "or the PESAFLOW4J_STATUS_URL environment variable.");
        }

        Map<String, String> query = new LinkedHashMap<>();
        query.put("apiClientID", gateway.getConfig().getApiClientId());
        query.put("serviceID", gateway.getConfig().getServiceId());
        query.put("billRefNumber", reference);
        if (extraParams != null) {
            query.putAll(extraParams);
        }

        HttpTransport.HttpResponse response = HttpTransport.get(statusUrl, query, gateway.getConfig().getConnectTimeoutMillis());
        return new PaymentStatusResult(response.requestUrl, response.httpStatus, response.body);
    }

    /** {@link #checkPaymentStatus(String, Map)} with no extra query parameters. */
    public PaymentStatusResult checkPaymentStatus(String reference) {
        return checkPaymentStatus(reference, null);
    }

    /**
     * Verifies a callback/notification payload from the gateway. Hand the
     * POST body / webhook payload straight to {@code verify(payload)}.
     */
    public VerifyResult verify(Map<String, String> callbackData) {
        String status = nullToEmpty(callbackData.get("status")).trim();
        boolean signatureValid = gateway.verifyNotificationHash(callbackData);
        boolean success = signatureValid && gateway.isSuccessStatus(status);

        String reference = firstNonBlank(callbackData.get("client_invoice_ref"), callbackData.get("billRefNumber"));
        BigDecimal amountPaid = parseAmount(firstNonBlank(callbackData.get("amount_paid"), callbackData.get("amount")));

        String description = callbackData.get("description") != null ? callbackData.get("description")
                : callbackData.get("billDesc") != null ? callbackData.get("billDesc")
                : success ? "Payment verified successfully" : "Payment verification failed";

        return new VerifyResult(success, signatureValid, status, reference.trim(), amountPaid, callbackData, description);
    }

    /** Shorthand for {@code verify(callbackData).isSuccess()}. */
    public boolean isPaid(Map<String, String> callbackData) {
        return verify(callbackData).isSuccess();
    }

    /** Access the underlying low-level signing/verification engine. */
    public Pesaflow4jGateway getGateway() {
        return gateway;
    }

    private static BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a;
        }
        return b != null ? b : "";
    }
}
