package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.ClaimUtils;
import com.github.novotnyr.idea.jwt.SignatureContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Jwt {
    public static Jwt EMPTY = new Jwt();

    private final JwtDecoder decoder = new JwtDecoder();

    private final List<NamedClaim<?>> headerClaims = new LinkedList<>();

    private final List<NamedClaim<?>> payloadClaims = new LinkedList<>();

    private SignatureContext signatureContext;

    private ParsedJwt parsedJwt;

    private String algorithm;

    public Jwt() {
    }

    public Jwt(String encodedJwt) {
        applySnapshot(this.decoder.parse(encodedJwt));
        this.algorithm = this.parsedJwt.getAlgorithm();
    }

    private void applySnapshot(ParsedJwt parsedJwt) {
        List<NamedClaim<?>> headers = new ArrayList<>();
        parsedJwt.header().properties().forEach(entry -> {
            JsonNode value = entry.getValue().deepCopy();
            headers.add(value.isTextual()
                    ? new StringClaim(entry.getKey(), value.textValue())
                    : new RawClaim(entry.getKey(), value));
        });
        List<NamedClaim<?>> payload = new ArrayList<>();
        parsedJwt.payload().properties().forEach(entry ->
                payload.add(ClaimUtils.toPayloadClaim(entry.getKey(), entry.getValue())));

        this.headerClaims.clear();
        this.headerClaims.addAll(headers);
        this.payloadClaims.clear();
        this.payloadClaims.addAll(payload);
        this.parsedJwt = parsedJwt;
    }

    public List<NamedClaim<?>> getHeaderClaims() {
        return Collections.unmodifiableList(this.headerClaims);
    }

    public List<NamedClaim<?>> getPayloadClaims() {
        return Collections.unmodifiableList(this.payloadClaims);
    }

    public void removeClaim(String claimName) {
        List<NamedClaim<?>> candidate = new ArrayList<>(this.payloadClaims);
        candidate.removeIf(claim -> claim.getName().equals(claimName));
        signPayload(candidate);
    }

    public void setPayloadClaim(NamedClaim<?> claim) {
        List<NamedClaim<?>> candidate = new ArrayList<>(this.payloadClaims);
        doSetClaim(claim, candidate);
        signPayload(candidate);
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

    private void signPayload(List<NamedClaim<?>> candidate) {
        JwtBuilder builder = new JwtBuilder().withHeaderClaims(this.headerClaims);
        for (NamedClaim<?> payloadClaim : candidate) {
            builder.withClaim(payloadClaim);
        }
        applySnapshot(this.decoder.parse(builder.sign(this.algorithm, this.signatureContext)));
    }

    public void rebuild() {
        signPayload(this.payloadClaims);
    }

    public void setSignatureContext(SignatureContext signatureContext) {
        this.signatureContext = signatureContext;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPayloadString() {
        return getDecodedJwt().getPayload();
    }

    /** Returns the decoded snapshot of the current encoded token. */
    public DecodedJWT getDecodedJwt() {
        if (this.parsedJwt == null) {
            throw new JWTDecodeException("The token is null");
        }
        return this.parsedJwt;
    }

    @Override
    public String toString() {
        return this.parsedJwt == null ? null : this.parsedJwt.getToken();
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
                Objects.equals(toString(), jwt.toString()) &&
                Objects.equals(getAlgorithm(), jwt.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHeaderClaims(), getPayloadClaims(), this.signatureContext, toString(), getAlgorithm());
    }

    public boolean isEmpty() {
        return Jwt.EMPTY.equals(this);
    }
}
