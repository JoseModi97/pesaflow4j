package io.github.josemodi97.pesaflow4j.internal;

import io.github.josemodi97.pesaflow4j.exception.Pesaflow4jTransportException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Java 11+ variant of {@code HttpTransport}, built on {@link HttpClient}
 * instead of {@link java.net.HttpURLConnection}: negotiates HTTP/2
 * automatically (falling back to HTTP/1.1), and is virtual-thread-friendly
 * on Java 21+.
 *
 * <p>Packaged into {@code META-INF/versions/11/} of the multi-release core
 * jar. A Java 11+ JVM loads this class transparently in place of the
 * Java-8 base version under {@code src/main/java}; the public API
 * (including the nested {@code HttpResponse} shape) is kept identical on
 * purpose, since callers must not need to know which variant is active.
 */
public final class HttpTransport {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private HttpTransport() {
    }

    public static HttpResponse postForm(String url, Map<String, String> fields, int timeoutMs) {
        String body = encodeForm(fields);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout(timeoutMs))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(request, url);
    }

    public static HttpResponse get(String url, Map<String, String> query, int timeoutMs) {
        String target = query.isEmpty() ? url : url + (url.contains("?") ? "&" : "?") + encodeForm(query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .timeout(timeout(timeoutMs))
                .GET()
                .build();
        return send(request, target);
    }

    private static HttpResponse send(HttpRequest request, String url) {
        try {
            java.net.http.HttpResponse<String> response = CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new HttpResponse(url, response.statusCode(), response.body());
        } catch (IOException e) {
            throw new Pesaflow4jTransportException("Request to " + url + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Pesaflow4jTransportException("Request to " + url + " was interrupted", new IOException(e));
        }
    }

    private static Duration timeout(int timeoutMs) {
        return Duration.ofMillis(timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS);
    }

    private static String encodeForm(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return sb.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** Plain HTTP response holder. Not part of the public API. */
    public static final class HttpResponse {
        public final String requestUrl;
        public final int httpStatus;
        public final String body;

        HttpResponse(String requestUrl, int httpStatus, String body) {
            this.requestUrl = requestUrl;
            this.httpStatus = httpStatus;
            this.body = body;
        }
    }
}
