package com.github.novotnyr.idea.jwt.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Date;

/** Seconds since the Unix epoch, including the fractional values allowed by RFC 7519. */
public final class NumericDate {
    private static final BigDecimal NANOSECOND = BigDecimal.ONE.movePointLeft(9);

    private static final BigDecimal MILLISECOND = BigDecimal.ONE.movePointLeft(3);

    private static final BigDecimal MIN_INSTANT_EXCLUSIVE = BigDecimal.valueOf(Instant.MIN.getEpochSecond())
            .subtract(NANOSECOND);

    private static final BigDecimal MAX_INSTANT_EXCLUSIVE = BigDecimal.valueOf(Instant.MAX.getEpochSecond())
            .add(BigDecimal.ONE);

    private static final BigDecimal MIN_DATE_EXCLUSIVE = BigDecimal.valueOf(Long.MIN_VALUE)
            .subtract(BigDecimal.ONE).movePointLeft(3);

    private static final BigDecimal MAX_DATE_EXCLUSIVE = BigDecimal.valueOf(Long.MAX_VALUE)
            .add(BigDecimal.ONE).movePointLeft(3);

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

    /** Returns a nanosecond-precision instant, or null when the timestamp exceeds {@link Instant}'s range. */
    public static Instant toInstant(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        if (seconds.compareTo(MIN_INSTANT_EXCLUSIVE) <= 0 || seconds.compareTo(MAX_INSTANT_EXCLUSIVE) >= 0) {
            return null;
        }
        if (seconds.abs().compareTo(NANOSECOND) < 0) {
            return Instant.EPOCH;
        }
        try {
            BigDecimal timestamp = seconds.setScale(9, RoundingMode.DOWN);
            BigDecimal epochSecondValue = timestamp.setScale(0, RoundingMode.FLOOR);
            long epochSecond = epochSecondValue.longValueExact();
            long nanos = timestamp.subtract(epochSecondValue).movePointRight(9).longValueExact();
            return Instant.ofEpochSecond(epochSecond, nanos);
        } catch (ArithmeticException | DateTimeException e) {
            return null;
        }
    }

    /** Returns null when the timestamp exceeds the display range of {@link Date}. */
    public static Date toDate(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        if (seconds.compareTo(MIN_DATE_EXCLUSIVE) <= 0 || seconds.compareTo(MAX_DATE_EXCLUSIVE) >= 0) {
            return null;
        }
        if (seconds.abs().compareTo(MILLISECOND) < 0) {
            return new Date(0);
        }
        try {
            long milliseconds = seconds.movePointRight(3).setScale(0, RoundingMode.DOWN).longValueExact();
            return new Date(milliseconds);
        } catch (ArithmeticException e) {
            return null;
        }
    }
}
