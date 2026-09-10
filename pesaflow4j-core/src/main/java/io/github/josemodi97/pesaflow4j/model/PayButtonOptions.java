package io.github.josemodi97.pesaflow4j.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cosmetic options for the HTML {@code <button>} rendered by
 * {@code Pesaflow4jClient.payButton(...)}. The underlying {@code <form>}'s
 * {@code target} attribute is also read from here.
 */
public final class PayButtonOptions {

    private String cssClass = "btn btn-primary";
    private String id;
    private String target = "_blank";
    private String style;
    private final Map<String, String> extraAttributes = new LinkedHashMap<>();

    public static PayButtonOptions create() {
        return new PayButtonOptions();
    }

    public PayButtonOptions cssClass(String cssClass) {
        this.cssClass = cssClass;
        return this;
    }

    public PayButtonOptions id(String id) {
        this.id = id;
        return this;
    }

    public PayButtonOptions target(String target) {
        this.target = target;
        return this;
    }

    public PayButtonOptions style(String style) {
        this.style = style;
        return this;
    }

    public PayButtonOptions attribute(String name, String value) {
        this.extraAttributes.put(name, value);
        return this;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getId() {
        return id;
    }

    public String getTarget() {
        return target;
    }

    public String getStyle() {
        return style;
    }

    public Map<String, String> getExtraAttributes() {
        return extraAttributes;
    }
}
