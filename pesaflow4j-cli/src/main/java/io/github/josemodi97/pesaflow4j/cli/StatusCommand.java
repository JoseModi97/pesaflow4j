package io.github.josemodi97.pesaflow4j.cli;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.model.PaymentStatusResult;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/** Polls the configured status endpoint for the settlement status of a previously submitted reference. */
@Command(name = "status", description = "Poll settlement status for a previously submitted invoice reference.")
public final class StatusCommand implements Callable<Integer> {

    @Mixin
    CredentialsMixin credentials;

    @Option(names = "--reference", required = true, description = "Invoice/order reference to check")
    String reference;

    @Override
    public Integer call() {
        Pesaflow4jClient client = new Pesaflow4jClient(credentials.toConfig());
        PaymentStatusResult result = client.checkPaymentStatus(reference);

        System.out.println("HTTP " + result.getHttpStatus());
        System.out.println(result.getResponseBody());
        return result.getHttpStatus() < 400 ? 0 : 1;
    }
}
