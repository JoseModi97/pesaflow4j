package io.github.josemodi97.pesaflow4j.jakarta;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Verifies an inbound PesaFlow webhook/IPN notification POSTed to a
 * {@code jakarta.servlet} endpoint (Tomcat 10+, Jetty 11+, Spring Boot 3,
 * or any plain Jakarta EE 9+ {@code HttpServlet}).
 *
 * <pre>{@code
 * public class NotifyServlet extends HttpServlet {
 *     private final Pesaflow4jServletWebhookHandler handler = new Pesaflow4jServletWebhookHandler(client)
 *             .onSuccess((result, req, res) -> markOrderPaid(result.getReference()))
 *             .onFailure((result, req, res) -> log.warn(result.getDescription()));
 *
 *     protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
 *         handler.handle(req, res);
 *     }
 * }
 * }</pre>
 */
public final class Pesaflow4jServletWebhookHandler {

    private final Pesaflow4jClient client;
    private WebhookSuccessCallback onSuccess;
    private WebhookFailureCallback onFailure;

    public Pesaflow4jServletWebhookHandler(Pesaflow4jClient client) {
        this.client = client;
    }

    public Pesaflow4jServletWebhookHandler onSuccess(WebhookSuccessCallback callback) {
        this.onSuccess = callback;
        return this;
    }

    public Pesaflow4jServletWebhookHandler onFailure(WebhookFailureCallback callback) {
        this.onFailure = callback;
        return this;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> payload = ServletParameterAdapter.toMap(request);
        VerifyResult result = client.verify(payload);

        if (result.isSuccess()) {
            if (onSuccess != null) {
                onSuccess.onSuccess(result, request, response);
            }
            if (!response.isCommitted()) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"status\":\"ok\"}");
            }
            return;
        }

        if (onFailure != null) {
            onFailure.onFailure(result, request, response);
        }
        if (!response.isCommitted()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"status\":\"error\",\"message\":\"" + escapeJson(result.getDescription()) + "\"}");
        }
    }

    private static void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
