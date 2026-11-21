package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes unmodified claims and preserves the encoded segments used to verify the signature.
 * A date accessor returns null when a claim cannot be represented as a date. The original
 * value remains available through {@link #getClaim(String)}.
 * <p>
 * {@link com.auth0.jwt.JWT#decode(String) Auth0's decoder} throws a {@link JWTDecodeException}
 * for timestamp claims such as {@code "iat": "yes"}.
 * Parsing the claims directly lets
 * the plugin display the original value and report a claim error.
 */
record ParsedJwt(String token, String encodedHeader, String encodedPayload, String signature,
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
