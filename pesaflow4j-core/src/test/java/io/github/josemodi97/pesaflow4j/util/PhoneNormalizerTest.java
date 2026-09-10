package io.github.josemodi97.pesaflow4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhoneNormalizerTest {

    @Test
    void normalizesLeadingZeroToCountryCode() {
        assertEquals("254712345678", PhoneNormalizer.normalize("0712345678"));
        assertEquals("254112345678", PhoneNormalizer.normalize("0112345678"));
    }

    @Test
    void stripsPlusPrefix() {
        assertEquals("254712345678", PhoneNormalizer.normalize("+254712345678"));
    }

    @Test
    void addsCountryCodeToShortLocalNumbers() {
        assertEquals("254712345678", PhoneNormalizer.normalize("712345678"));
        assertEquals("254112345678", PhoneNormalizer.normalize("112345678"));
    }

    @Test
    void leavesAlreadyNormalizedNumbersUnchanged() {
        assertEquals("254712345678", PhoneNormalizer.normalize("254712345678"));
    }

    @Test
    void returnsEmptyStringForNull() {
        assertEquals("", PhoneNormalizer.normalize(null));
    }

    @Test
    void validatesStkPushEligiblePhones() {
        assertTrue(PhoneNormalizer.isValidStkPhone("254712345678"));
        assertTrue(PhoneNormalizer.isValidStkPhone("254112345678"));
        assertFalse(PhoneNormalizer.isValidStkPhone("254212345678"));
        assertFalse(PhoneNormalizer.isValidStkPhone("07123"));
    }
}
