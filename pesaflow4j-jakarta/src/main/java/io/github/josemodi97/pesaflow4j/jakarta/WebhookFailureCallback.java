package io.github.josemodi97.pesaflow4j.jakarta;

import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Invoked by {@link Pesaflow4jServletWebhookHandler} when a notification fails verification. */
@FunctionalInterface
public interface WebhookFailureCallback {
    void onFailure(VerifyResult result, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
