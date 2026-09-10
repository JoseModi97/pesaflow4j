package io.github.josemodi97.pesaflow4j.internal;

import io.github.josemodi97.pesaflow4j.exception.Pesaflow4jTransportException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Minimal, zero-dependency HTTP client built on {@link HttpURLConnection}
 * &mdash; part of the JDK since Java 1.1 &mdash; so the core module never
 * pulls in a third-party HTTP library. Not part of the public API.
 */
public final class HttpTransport {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private HttpTransport() {
    }

    public static HttpResponse postForm(String url, Map<String, String> fields, int timeoutMs) {
        String body = encodeForm(fields);
        return request(url, "POST", body, timeoutMs);
    }

    public static HttpResponse get(String url, Map<String, String> query, int timeoutMs) {
        String target = query.isEmpty() ? url : url + (url.contains("?") ? "&" : "?") + encodeForm(query);
        return request(target, "GET", null, timeoutMs);
    }

    private static HttpResponse request(String url, String method, String body, int timeoutMs) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS);
            connection.setReadTimeout(timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);

            if (body != null) {
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setRequestProperty("Content-Length", String.valueOf(payload.length));
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(payload);
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = stream == null ? "" : readAll(stream);

            return new HttpResponse(url, status, responseBody);
        } catch (IOException e) {
            throw new Pesaflow4jTransportException("Request to " + url + " failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
        }
        return sb.toString();
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
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (IOException e) {
            // UTF-8 is guaranteed to be supported by every JVM; unreachable.
            throw new IllegalStateException(e);
        }
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
