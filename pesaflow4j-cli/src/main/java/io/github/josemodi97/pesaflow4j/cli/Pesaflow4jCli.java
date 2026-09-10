package io.github.josemodi97.pesaflow4j.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Entry point for the PesaFlow4J command-line tool: build/submit signed
 * checkouts, poll settlement status, and verify captured webhook payloads
 * without writing any application code.
 */
@Command(
        name = "pesaflow4j",
        mixinStandardHelpOptions = true,
        version = "PesaFlow4J CLI",
        description = "Command-line companion to the PesaFlow4J Java SDK.",
        subcommands = {CheckoutCommand.class, StatusCommand.class, VerifyCommand.class})
public final class Pesaflow4jCli implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Pesaflow4jCli()).execute(args);
        System.exit(exitCode);
    }
}
