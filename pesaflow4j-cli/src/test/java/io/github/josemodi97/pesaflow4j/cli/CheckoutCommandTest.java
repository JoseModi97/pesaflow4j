package io.github.josemodi97.pesaflow4j.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CheckoutCommandTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void captureStdout() throws UnsupportedEncodingException {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, "UTF-8"));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void printsASignedPayloadWithoutSubmitting() throws UnsupportedEncodingException {
        int exitCode = new CommandLine(new Pesaflow4jCli()).execute(
                "checkout",
                "--client-id", "CID1", "--api-key", "KEY1", "--secret", "SECRET1", "--service-id", "SID1",
                "--amount", "500", "--reference", "INV-0001", "--description", "School fees",
                "--name", "Jane Doe", "--id-number", "12345678", "--phone", "0712345678");

        String output = capturedOut.toString("UTF-8");

        assertEquals(0, exitCode);
        assertTrue(output.contains("URL: "));
        assertTrue(output.contains("clientMSISDN = 254712345678"));
        assertTrue(output.contains("secureHash = "));
    }

    @Test
    void failsFastWhenARequiredPaymentFlagIsMissing() {
        int exitCode = new CommandLine(new Pesaflow4jCli()).execute(
                "checkout",
                "--client-id", "CID1", "--api-key", "KEY1", "--secret", "SECRET1", "--service-id", "SID1",
                "--amount", "500", "--reference", "INV-0001");

        assertTrue(exitCode != 0);
    }
}
