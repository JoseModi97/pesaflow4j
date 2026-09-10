package io.github.josemodi97.pesaflow4j.exception;

/**
 * Thrown when a {@link io.github.josemodi97.pesaflow4j.Pesaflow4jConfig}
 * is missing a required setting (client ID, API key, secret, service ID, etc.).
 */
public class Pesaflow4jConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Pesaflow4jConfigurationException(String message) {
        super(message);
    }
}
