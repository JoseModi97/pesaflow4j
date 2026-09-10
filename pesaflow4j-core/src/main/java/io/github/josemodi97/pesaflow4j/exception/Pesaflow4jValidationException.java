package io.github.josemodi97.pesaflow4j.exception;

/**
 * Thrown when a payment request is missing a required field
 * (amount, reference, description, name, idNumber).
 */
public class Pesaflow4jValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Pesaflow4jValidationException(String message) {
        super(message);
    }
}
