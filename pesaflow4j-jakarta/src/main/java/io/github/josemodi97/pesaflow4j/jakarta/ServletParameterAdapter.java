package io.github.josemodi97.pesaflow4j.jakarta;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts a servlet request's parameters (form-encoded POST body or query
 * string) into the plain {@code Map<String,String>} shape
 * {@code Pesaflow4jClient.verify(...)} expects.
 */
public final class ServletParameterAdapter {

    private ServletParameterAdapter() {
    }

    public static Map<String, String> toMap(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, String[]> parameters = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                result.put(entry.getKey(), values[0]);
            }
        }
        return result;
    }
}
