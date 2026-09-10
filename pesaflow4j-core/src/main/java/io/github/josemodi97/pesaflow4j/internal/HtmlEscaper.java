package io.github.josemodi97.pesaflow4j.internal;

/** Minimal HTML escaping for the generated pay-button form. Not part of the public API. */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String escapeAttribute(String value) {
        return escapeText(value).replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String escapeText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
