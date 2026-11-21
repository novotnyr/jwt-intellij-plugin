package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.novotnyr.idea.jwt.ClaimUtils;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.JwtDecoder.ParsedJwt;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        this.parsedJwt = this.decoder.parse(encodedJwt);

        readHeaderClaims(this.parsedJwt);

        this.algorithm = this.parsedJwt.getAlgorithm();

        for (Map.Entry<String, Claim> entry : this.parsedJwt.getClaims().entrySet()) {
            String name = entry.getKey();
            Claim claim = entry.getValue();
            JsonNode rawValue = null;
            if (ClaimUtils.isDateClaim(name)) {
                rawValue = claim.isNull() ? NullNode.getInstance() : claim.as(JsonNode.class);
            }
            this.payloadClaims.add(ClaimUtils.getClaim(name, claim, rawValue));
        }
    }

    private void readHeaderClaims(ParsedJwt parsedJwt) {
        this.headerClaims.clear();
        parsedJwt.header().properties().forEach(entry -> {
            JsonNode value = entry.getValue().deepCopy();
            this.headerClaims.add(value.isTextual()
                    ? new StringClaim(entry.getKey(), value.textValue())
                    : new RawClaim(entry.getKey(), value));
        });
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
        updatePayload();
    }


    public void setPayloadClaim(NamedClaim<?> claim) {
        doSetClaim(claim, this.payloadClaims);
        updatePayload();
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

    private void updatePayload() {
        JwtBuilder builder = new JwtBuilder().withHeaderClaims(this.headerClaims);
        for (NamedClaim<?> payloadClaim : this.payloadClaims) {
            builder.withClaim(payloadClaim);
        }
        ParsedJwt signedJwt = this.decoder.parse(builder.sign(this.algorithm, this.signatureContext));
        readHeaderClaims(signedJwt);
        this.parsedJwt = signedJwt;
    }

    public void rebuild() {
        updatePayload();
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
