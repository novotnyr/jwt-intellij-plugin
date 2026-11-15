package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/** Seconds since the Unix epoch, including the fractional values allowed by RFC 7519. */
public final class NumericDate {
    private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    private NumericDate() {
    }

    /** Reads the original payload so fractional NumericDates keep their JSON precision. */
    public static JsonNode parsePayload(DecodedJWT jwt) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(jwt.getPayload());
            JsonNode claims = PAYLOAD_MAPPER.readTree(payload);
            if (claims == null || !claims.isObject()) {
                throw new JWTDecodeException("The JWT payload must be a JSON object");
            }
            return claims;
        } catch (IOException | IllegalArgumentException e) {
            throw new JWTDecodeException("Cannot decode the JWT payload", e);
        }
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
