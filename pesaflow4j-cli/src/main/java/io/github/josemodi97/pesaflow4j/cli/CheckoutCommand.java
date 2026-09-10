package io.github.josemodi97.pesaflow4j.cli;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.model.CheckoutRequest;
import io.github.josemodi97.pesaflow4j.model.CheckoutResult;
import io.github.josemodi97.pesaflow4j.model.PaymentSubmissionResult;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Builds (and optionally submits) a signed checkout payload from the
 * command line — for smoke-testing credentials/signing without writing any
 * application code, or for scripting headless payments from CI/CD.
 */
@Command(name = "checkout", description = "Build a signed checkout payload, optionally submitting it directly.")
public final class CheckoutCommand implements Callable<Integer> {

    @Mixin
    CredentialsMixin credentials;

    @Option(names = "--amount", required = true, description = "Amount to charge, e.g. 500")
    double amount;

    @Option(names = "--reference", required = true, description = "Unique invoice/order reference")
    String reference;

    @Option(names = "--description", required = true, description = "What the payment is for")
    String description;

    @Option(names = "--name", required = true, description = "Payer's full name")
    String name;

    @Option(names = "--id-number", required = true, description = "Payer's National ID / Passport number")
    String idNumber;

    @Option(names = "--phone", description = "Payer's phone number, any Kenyan format")
    String phone;

    @Option(names = "--email", description = "Payer's email address")
    String email;

    @Option(names = "--callback-url", description = "Browser return URL on success")
    String callbackUrl;

    @Option(names = "--notify-url", description = "Server-to-server webhook/IPN URL")
    String notifyUrl;

    @Option(names = "--send-stk", description = "Request an M-Pesa STK push")
    boolean sendStk;

    @Option(names = "--submit", description = "POST the signed payload to the gateway instead of only printing it")
    boolean submit;

    @Override
    public Integer call() {
        Pesaflow4jClient client = new Pesaflow4jClient(credentials.toConfig());

        CheckoutRequest.Builder requestBuilder = CheckoutRequest.builder()
                .amount(amount)
                .reference(reference)
                .description(description)
                .name(name)
                .idNumber(idNumber)
                .sendStkPush(sendStk);
        if (phone != null) {
            requestBuilder.phone(phone);
        }
        if (email != null) {
            requestBuilder.email(email);
        }
        if (callbackUrl != null) {
            requestBuilder.callbackUrl(callbackUrl);
        }
        if (notifyUrl != null) {
            requestBuilder.notifyUrl(notifyUrl);
        }
        CheckoutRequest request = requestBuilder.build();

        if (!submit) {
            CheckoutResult result = client.checkout(request);
            System.out.println("URL: " + result.getUrl());
            System.out.println("Payload:");
            for (Map.Entry<String, String> field : result.getPayload().entrySet()) {
                System.out.println("  " + field.getKey() + " = " + field.getValue());
            }
            return 0;
        }

        PaymentSubmissionResult submission = client.initiatePayment(request);
        System.out.println("HTTP " + submission.getHttpStatus());
        System.out.println(submission.getResponseBody());
        return submission.getHttpStatus() < 400 ? 0 : 1;
    }
}
