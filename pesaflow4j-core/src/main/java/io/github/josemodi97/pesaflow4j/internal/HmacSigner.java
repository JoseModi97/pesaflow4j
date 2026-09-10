package io.github.josemodi97.pesaflow4j.internal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Internal HMAC-SHA256 helper. Not part of the public API.
 *
 * <p>The gateway's {@code secureHash} is {@code base64(hex(hmac_sha256(data, key)))}:
 * a hex-encoded HMAC digest, whose UTF-8 bytes are then Base64-encoded a
 * second time. This is a common convention among PHP-based HMAC hashing
 * (PHP's {@code hash_hmac()} returns hex by default). Base64-encoding the
 * raw digest bytes directly &mdash; the more "natural" Java approach &mdash;
 * produces a different, incompatible hash, so both encoding steps are
 * reproduced explicitly here.
 */
public final class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HmacSigner() {
    }

    public static String signHexThenBase64(String data, String key) {
        byte[] digest = hmacSha256(data, key);
        String hex = toHex(digest);
        return java.util.Base64.getEncoder().encodeToString(hex.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // HmacSHA256 is guaranteed present on every conforming JVM (Java 8+),
            // and the key is always non-null/non-empty by the time this runs.
            throw new IllegalStateException("Unable to compute HMAC-SHA256 signature", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }

    /**
     * Constant-time comparison of two ASCII/UTF-8 strings, to avoid leaking
     * timing information when checking an inbound signature against the
     * expected one.
     */
    public static boolean timingSafeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < x.length; i++) {
            diff |= x[i] ^ y[i];
        }
        return diff == 0;
    }
}
