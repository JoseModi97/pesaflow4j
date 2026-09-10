package io.github.josemodi97.pesaflow4j.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.Pesaflow4jConfig;
import io.github.josemodi97.pesaflow4j.internal.HmacSigner;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class Pesaflow4jServletWebhookHandlerTest {

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

    private static HttpServletRequest requestWithParams(Map<String, String> params) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Hashtable<String, String[]> parameterMap = new Hashtable<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            parameterMap.put(entry.getKey(), new String[] {entry.getValue()});
        }
        when(request.getParameterMap()).thenReturn(parameterMap);
        return request;
    }

    @Test
    void invokesOnSuccessAndWritesOkForAValidNotification() throws IOException {
        Map<String, String> notification = new java.util.LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("amount_paid", "500.00");
        notification.put("status", "settled");
        notification.put("secure_hash", sign(notification));

        HttpServletRequest request = requestWithParams(notification);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        boolean[] successCalled = {false};
        new Pesaflow4jServletWebhookHandler(client())
                .onSuccess((result, req, res) -> successCalled[0] = true)
                .onFailure((result, req, res) -> {
                    throw new AssertionError("onFailure should not be called for a valid notification");
                })
                .handle(request, response);

        assertEquals(true, successCalled[0]);
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    void invokesOnFailureForATamperedNotification() throws IOException {
        Map<String, String> notification = new java.util.LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("amount_paid", "999999.00");
        notification.put("status", "settled");
        notification.put("secure_hash", "not-a-real-signature");

        HttpServletRequest request = requestWithParams(notification);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        boolean[] failureCalled = {false};
        new Pesaflow4jServletWebhookHandler(client())
                .onSuccess((result, req, res) -> {
                    throw new AssertionError("onSuccess should not be called for a tampered notification");
                })
                .onFailure((result, req, res) -> failureCalled[0] = true)
                .handle(request, response);

        assertEquals(true, failureCalled[0]);
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doesNotOverwriteAResponseTheCallbackAlreadyCommitted() throws IOException {
        Map<String, String> notification = new java.util.LinkedHashMap<>();
        notification.put("client_invoice_ref", "INV-0001");
        notification.put("amount_paid", "500.00");
        notification.put("status", "settled");
        notification.put("secure_hash", sign(notification));

        HttpServletRequest request = requestWithParams(notification);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        new Pesaflow4jServletWebhookHandler(client())
                .onSuccess((result, req, res) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .handle(request, response);

        verify(response, never()).setStatus(HttpServletResponse.SC_OK);
        verify(response, never()).getWriter();
    }
}
