package io.github.josemodi97.pesaflow4j.exception;

import java.io.IOException;

/**
 * Wraps a lower-level {@link IOException} raised while talking to the
 * payment gateway over HTTP (connect timeout, DNS failure, TLS error, etc.).
 */
public class Pesaflow4jTransportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Pesaflow4jTransportException(String message, IOException cause) {
        super(message, cause);
    }
}
