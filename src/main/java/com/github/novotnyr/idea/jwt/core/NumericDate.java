package com.github.novotnyr.idea.jwt.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Date;

/** Seconds since the Unix epoch, including the fractional values allowed by RFC 7519. */
public final class NumericDate {
    private NumericDate() {
    }

    /** Returns null for a missing, null, or non-numeric JSON claim. */
    public static BigDecimal fromJson(JsonNode claim) {
        return claim != null && claim.isNumber() ? claim.decimalValue() : null;
    }

    public static BigDecimal fromInstant(Instant instant) {
        return BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
    }

    /** Returns null when the timestamp exceeds the display range of {@link Date}. */
    public static Date toDate(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        try {
            long milliseconds = seconds.movePointRight(3).setScale(0, RoundingMode.DOWN).longValueExact();
            return new Date(milliseconds);
        } catch (ArithmeticException e) {
            return null;
        }
    }
}
