package io.github.josemodi97.pesaflow4j.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.pesaflow4j.internal.HmacSigner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class VerifyCommandTest {

    private static final String API_KEY = "APIKEY1";
    private static final String SECRET = "SECRET1";

    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void captureStdout() throws UnsupportedEncodingException {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, "UTF-8"));
    }

    @AfterEach
    void restoreStdio() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    private static String sign(String reference, String amountPaid) {
        String data = reference + "" + amountPaid + "" + SECRET;
        return HmacSigner.signHexThenBase64(data, API_KEY);
    }

    @Test
    void verifiesAValidNotificationPassedAsDataFlags() throws UnsupportedEncodingException {
        String hash = sign("INV-0001", "500.00");

        int exitCode = new CommandLine(new Pesaflow4jCli()).execute(
                "verify",
                "--client-id", "CID1", "--api-key", API_KEY, "--secret", SECRET, "--service-id", "SID1",
                "--data", "client_invoice_ref=INV-0001",
                "--data", "amount_paid=500.00",
                "--data", "status=settled",
                "--data", "secure_hash=" + hash);

        String output = capturedOut.toString("UTF-8");

        assertEquals(0, exitCode);
        assertTrue(output.contains("success        = true"));
        assertTrue(output.contains("reference      = INV-0001"));
    }

    @Test
    void rejectsATamperedNotificationReadFromStdin() throws UnsupportedEncodingException {
        String body = "client_invoice_ref=INV-0001&amount_paid=999999.00&status=settled&secure_hash=bogus";
        System.setIn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        int exitCode = new CommandLine(new Pesaflow4jCli()).execute(
                "verify",
                "--client-id", "CID1", "--api-key", API_KEY, "--secret", SECRET, "--service-id", "SID1",
                "--stdin");

        String output = capturedOut.toString("UTF-8");

        assertTrue(exitCode != 0);
        assertTrue(output.contains("signatureValid = false"));
    }
}
