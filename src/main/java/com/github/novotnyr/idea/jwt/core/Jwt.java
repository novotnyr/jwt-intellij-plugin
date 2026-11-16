package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.novotnyr.idea.jwt.ClaimUtils;
import com.github.novotnyr.idea.jwt.SignatureContext;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Jwt {
    public static Jwt EMPTY = new Jwt();

    private static final ObjectMapper HEADER_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    private final List<NamedClaim<?>> headerClaims = new LinkedList<>();

    private final List<NamedClaim<?>> payloadClaims = new LinkedList<>();

    private SignatureContext signatureContext;

    private String jwtString;

    private String algorithm;

    public Jwt() {
    }

    public Jwt(String encodedJwt) {
        this.jwtString = encodedJwt;

        DecodedJWT decodedJwt = JWT.decode(encodedJwt);

        readHeaderClaims(decodedJwt);

        this.algorithm = decodedJwt.getAlgorithm();

        JsonNode rawPayload = null;
        for (Map.Entry<String, Claim> claim : getClaims(decodedJwt).entrySet()) {
            if (rawPayload == null && ClaimUtils.isDateClaim(claim.getKey(), claim.getValue())) {
                rawPayload = NumericDate.parsePayload(decodedJwt);
            }
            JsonNode rawValue = rawPayload == null ? null : rawPayload.get(claim.getKey());
            this.payloadClaims.add(ClaimUtils.getClaim(claim.getKey(), claim.getValue(), rawValue));
        }
    }

    private Map<String, Claim> getClaims(DecodedJWT decodedJwt) {
        return decodedJwt.getClaims();
    }

    private void readHeaderClaims(DecodedJWT decodedJwt) {
        try {
            JsonNode header = HEADER_MAPPER.readTree(Base64.getUrlDecoder().decode(decodedJwt.getHeader()));
            if (header == null || !header.isObject()) {
                throw new JWTDecodeException("JWT header must be a JSON object");
            }
            this.headerClaims.clear();
            header.properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                this.headerClaims.add(value.isTextual()
                        ? new StringClaim(entry.getKey(), value.textValue())
                        : new RawClaim(entry.getKey(), value));
            });
        } catch (IOException | IllegalArgumentException e) {
            throw new JWTDecodeException("Cannot decode JWT header", e);
        }
    }

    public List<NamedClaim<?>> getHeaderClaims() {
        return Collections.unmodifiableList(this.headerClaims);
    }

    public List<NamedClaim<?>> getPayloadClaims() {
        return Collections.unmodifiableList(this.payloadClaims);
    }

    public void removeClaim(String claimName) {
        Iterator<NamedClaim<?>> iterator = this.payloadClaims.iterator();
        while(iterator.hasNext()) {
            NamedClaim<?> claim = iterator.next();
            if (claim.getName().equals(claimName)) {
                iterator.remove();
                break;
            }
        }
        updatePayload(null);
    }


    public void setPayloadClaim(NamedClaim<?> claim) {
        doSetClaim(claim, this.payloadClaims);
        updatePayload(claim);
    }

    public void setHeaderClaim(NamedClaim<?> claim) {
        doSetClaim(claim, this.headerClaims);
    }

    private void doSetClaim(NamedClaim<?> claim, List<NamedClaim<?>> destinationClaimList) {
        int i = 0;
        boolean found = false;
        for (NamedClaim<?> payloadClaim : destinationClaimList) {
            if(payloadClaim.getName().equals(claim.getName())) {
                found = true;
                break;
            }
            i++;
        }
        if(found) {
            destinationClaimList.set(i, claim);
        } else {
            destinationClaimList.add(claim);
        }
    }

    private void updatePayload(NamedClaim<?> claim) {
        JwtBuilder builder = new JwtBuilder().withHeaderClaims(this.headerClaims);
        for (NamedClaim<?> payloadClaim : this.payloadClaims) {
            builder.withClaim(payloadClaim);
        }
        this.jwtString = builder.sign(this.algorithm, this.signatureContext);
        readHeaderClaims(JWT.decode(this.jwtString));
    }

    public void rebuild() {
        updatePayload(null);
    }

    public void setSignatureContext(SignatureContext signatureContext) {
        this.signatureContext = signatureContext;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPayloadString() {
        return splitToken(this.jwtString)[1];
    }

    protected String[] splitToken(String token) throws JWTDecodeException {
        String[] parts = token.split("\\.");
        if (parts.length == 2 && token.endsWith(".")) {
            //Tokens with alg='none' have empty String as Signature.
            parts = new String[]{ parts[0], parts[1], "" };
        }
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expecting 3 token parts, but got " + parts.length);
        }
        return parts;
    }

    public String toString() {
        return this.jwtString;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jwt)) return false;
        Jwt jwt = (Jwt) o;
        return Objects.equals(getHeaderClaims(), jwt.getHeaderClaims()) &&
                Objects.equals(getPayloadClaims(), jwt.getPayloadClaims()) &&
                Objects.equals(this.signatureContext, jwt.signatureContext) &&
                Objects.equals(this.jwtString, jwt.jwtString) &&
                Objects.equals(getAlgorithm(), jwt.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHeaderClaims(), getPayloadClaims(), this.signatureContext, this.jwtString, getAlgorithm());
    }

    public boolean isEmpty() {
        return Jwt.EMPTY.equals(this);
    }
}
