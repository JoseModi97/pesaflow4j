package io.github.josemodi97.pesaflow4j.cli;

import io.github.josemodi97.pesaflow4j.Pesaflow4jClient;
import io.github.josemodi97.pesaflow4j.model.VerifyResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Verifies a captured webhook/IPN notification payload against the
 * configured merchant secret — useful for reproducing and debugging a
 * signature mismatch reported in production, without a live gateway call.
 */
@Command(name = "verify", description = "Verify a captured notification payload's HMAC signature and status.")
public final class VerifyCommand implements Callable<Integer> {

    @Mixin
    CredentialsMixin credentials;

    @Option(names = "--data", description = "A key=value notification field. Repeatable.")
    Map<String, String> data = new LinkedHashMap<>();

    @Option(names = "--stdin", description = "Read a form-encoded (key=value&key=value) payload from stdin")
    boolean fromStdin;

    @Override
    public Integer call() throws IOException {
        Map<String, String> payload = new LinkedHashMap<>(data);
        if (fromStdin) {
            payload.putAll(readFormEncoded(System.in));
        }

        Pesaflow4jClient client = new Pesaflow4jClient(credentials.toConfig());
        VerifyResult result = client.verify(payload);

        System.out.println("signatureValid = " + result.isSignatureValid());
        System.out.println("success        = " + result.isSuccess());
        System.out.println("status         = " + result.getStatus());
        System.out.println("reference      = " + result.getReference());
        System.out.println("amountPaid     = " + result.getAmountPaid());
        System.out.println("description    = " + result.getDescription());

        return result.isSuccess() ? 0 : 1;
    }

    private static Map<String, String> readFormEncoded(java.io.InputStream in) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                body.append(buf, 0, read);
            }
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        for (String pair : body.toString().trim().split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            parsed.put(urlDecode(key), urlDecode(value));
        }
        return parsed;
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
