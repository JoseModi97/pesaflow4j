package io.github.josemodi97.pesaflow4j.servlet;

import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Invoked by {@link Pesaflow4jServletWebhookHandler} when a notification verifies successfully. */
@FunctionalInterface
public interface WebhookSuccessCallback {
    void onSuccess(VerifyResult result, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
