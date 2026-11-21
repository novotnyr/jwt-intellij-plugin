package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.novotnyr.idea.jwt.validation.JwtClaimsValidator;
import com.github.novotnyr.idea.jwt.validation.JwtErrors;

import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Decodes the original JWT claims and reports validation errors alongside them. */
public final class JwtDecoder {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    private final JwtClaimsValidator claimsValidator;

    public JwtDecoder() {
        this(Clock.systemUTC());
    }

    public JwtDecoder(Clock clock) {
        this.claimsValidator = new JwtClaimsValidator(clock);
    }

    public JsonWebToken decode(String token) {
        if (token == null) {
            throw new JWTDecodeException("The token is null");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new JWTDecodeException("The JWT must have three parts");
        }

        ObjectNode header = parseHeader(parts[0]);
        ObjectNode payload = parsePayload(parts[1]);
        DecodedJWT decoded = new ParsedJwt(token, parts[0], parts[1], parts[2], header, payload);
        return new JsonWebToken(decoded, new JwtErrors(this.claimsValidator.validate(decoded), null));
    }

    ObjectNode parseHeader(String encodedHeader) {
        return parseObject(encodedHeader, "header");
    }

    /** Parses a Base64URL payload segment as JSON with full numeric precision. */
    public ObjectNode parsePayload(String encodedPayload) {
        return parseObject(encodedPayload, "payload");
    }

    private ObjectNode parseObject(String encoded, String partName) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encoded);
            if (MAPPER.readTree(json) instanceof ObjectNode claims) {
                return claims;
            }
            throw new JWTDecodeException("The JWT " + partName + " must be a JSON object");
        } catch (IOException | IllegalArgumentException e) {
            throw new JWTDecodeException("Cannot decode the JWT " + partName, e);
        }
    }

    /**
     * Exposes unmodified claims and preserves the encoded segments used to verify the signature.
     * A date accessor returns null when a claim cannot be represented as a date. The original
     * value remains available through {@link #getClaim(String)}.
     */
    private record ParsedJwt(String token, String encodedHeader, String encodedPayload, String signature,
                             ObjectNode header, ObjectNode payload) implements DecodedJWT {
        @Override
        public String getToken() {
            return this.token;
        }

        @Override
        public String getHeader() {
            return this.encodedHeader;
        }

        @Override
        public String getPayload() {
            return this.encodedPayload;
        }

        @Override
        public String getSignature() {
            return this.signature;
        }

        @Override
        public String getAlgorithm() {
            return this.header.path(HeaderParams.ALGORITHM).asText(null);
        }

        @Override
        public String getType() {
            return this.header.path(HeaderParams.TYPE).asText(null);
        }

        @Override
        public String getContentType() {
            return this.header.path(HeaderParams.CONTENT_TYPE).asText(null);
        }

        @Override
        public String getKeyId() {
            return this.header.path(HeaderParams.KEY_ID).asText(null);
        }

        @Override
        public Claim getHeaderClaim(String name) {
            return new JsonClaim(this.header.path(name));
        }

        @Override
        public String getIssuer() {
            return this.payload.path(RegisteredClaims.ISSUER).asText(null);
        }

        @Override
        public String getSubject() {
            return this.payload.path(RegisteredClaims.SUBJECT).asText(null);
        }

        @Override
        public List<String> getAudience() {
            Claim audience = getClaim(RegisteredClaims.AUDIENCE);
            String singleAudience = audience.asString();
            return singleAudience == null ? audience.asList(String.class) : List.of(singleAudience);
        }

        @Override
        public Date getExpiresAt() {
            return getClaim(RegisteredClaims.EXPIRES_AT).asDate();
        }

        @Override
        public Instant getExpiresAtAsInstant() {
            return getClaim(RegisteredClaims.EXPIRES_AT).asInstant();
        }

        @Override
        public Date getNotBefore() {
            return getClaim(RegisteredClaims.NOT_BEFORE).asDate();
        }

        @Override
        public Instant getNotBeforeAsInstant() {
            return getClaim(RegisteredClaims.NOT_BEFORE).asInstant();
        }

        @Override
        public Date getIssuedAt() {
            return getClaim(RegisteredClaims.ISSUED_AT).asDate();
        }

        @Override
        public Instant getIssuedAtAsInstant() {
            return getClaim(RegisteredClaims.ISSUED_AT).asInstant();
        }

        @Override
        public String getId() {
            return this.payload.path(RegisteredClaims.JWT_ID).asText(null);
        }

        @Override
        public Claim getClaim(String name) {
            return new JsonClaim(this.payload.path(name));
        }

        @Override
        public Map<String, Claim> getClaims() {
            Map<String, Claim> claims = new LinkedHashMap<>();
            this.payload.properties().forEach(entry -> claims.put(entry.getKey(), new JsonClaim(entry.getValue())));
            return Collections.unmodifiableMap(claims);
        }
    }

    private record JsonClaim(JsonNode value) implements Claim {
        @Override
        public boolean isNull() {
            return this.value.isNull();
        }

        @Override
        public boolean isMissing() {
            return this.value.isMissingNode();
        }

        @Override
        public Boolean asBoolean() {
            return this.value.isBoolean() ? this.value.booleanValue() : null;
        }

        @Override
        public Integer asInt() {
            return this.value.isNumber() ? this.value.intValue() : null;
        }

        @Override
        public Long asLong() {
            return this.value.isNumber() ? this.value.longValue() : null;
        }

        @Override
        public Double asDouble() {
            return this.value.isNumber() ? this.value.doubleValue() : null;
        }

        @Override
        public String asString() {
            return this.value.textValue();
        }

        @Override
        public Date asDate() {
            Instant instant = asInstant();
            try {
                return instant == null ? null : Date.from(instant);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public Instant asInstant() {
            if (!this.value.canConvertToLong()) {
                return null;
            }
            try {
                return Instant.ofEpochSecond(this.value.longValue());
            } catch (DateTimeException e) {
                return null;
            }
        }

        @Override
        public <T> T[] asArray(Class<T> type) {
            return this.value.isArray() ? readAs(MAPPER.getTypeFactory().constructArrayType(type)) : null;
        }

        @Override
        public <T> List<T> asList(Class<T> type) {
            return this.value.isArray() ? readAs(MAPPER.getTypeFactory().constructCollectionType(List.class, type)) : null;
        }

        @Override
        public Map<String, Object> asMap() {
            return this.value.isObject()
                    ? readAs(MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class))
                    : null;
        }

        @Override
        public <T> T as(Class<T> type) {
            if (isNull() || isMissing()) {
                return null;
            }
            if (JsonNode.class.isAssignableFrom(type) && type.isInstance(this.value)) {
                return type.cast(this.value.deepCopy());
            }
            return readAs(MAPPER.constructType(type));
        }

        @Override
        public String toString() {
            return this.value.toString();
        }

        private <T> T readAs(JavaType type) {
            try {
                return MAPPER.readerFor(type).readValue(this.value);
            } catch (IOException e) {
                throw new JWTDecodeException("Cannot read JWT claim as " + type, e);
            }
        }
    }
}
