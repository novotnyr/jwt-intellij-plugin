package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.impl.NullClaim;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.ClaimUtils;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Jwt {
    public static Jwt EMPTY = new Jwt();

    private List<NamedClaim<?>> headerClaims = new LinkedList<>();

    private List<NamedClaim<?>> payloadClaims = new LinkedList<>();

    private SigningCredentials signingCredentials;

    private String jwtString;

    private String algorithm;

    public Jwt() {
    }

    public Jwt(String encodedJwt) {
        this.jwtString = encodedJwt;

        DecodedJWT decodedJwt = JWT.decode(encodedJwt);

        addHeaderIfNotNull("alg", decodedJwt.getAlgorithm());
        addHeaderIfNotNull("typ", decodedJwt.getType());
        addHeaderIfNotNull("cty", decodedJwt.getContentType());
        addHeaderIfNotNull("kid", decodedJwt.getKeyId());

        this.algorithm = decodedJwt.getAlgorithm();

        for (Map.Entry<String, Claim> claim : getClaims(decodedJwt).entrySet()) {
            this.payloadClaims.add(ClaimUtils.getClaim(claim.getKey(), claim.getValue()));
        }
    }

    private Map<String, Claim> getClaims(DecodedJWT decodedJwt) {
        try {
            if (Hacking.isInstanceOfJWTDecoder(decodedJwt)) {
                return Hacking.getClaims(decodedJwt);
            }
        } catch (Exception e) {
            // do nothing, fall back to the regular and clean claim retrieval
            e.printStackTrace();
        }
        return decodedJwt.getClaims();
    }

    private static Claim _claimFromNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return new NullClaim();
        }
        return _newJsonNodeClaim(node);
    }

    private static Claim _newJsonNodeClaim(JsonNode node) {
        try {
            //noinspection JavaReflectionMemberAccess
            Constructor<?> constructor = Class.forName("com.auth0.jwt.impl.JsonNodeClaim")
                    .getConstructor(JsonNode.class);
            return (Claim) constructor.newInstance(node);
        } catch (Exception e) {
            return new NullClaim();
        }
    }

    private void addHeaderIfNotNull(String claimName, String value) {
        if(value == null) {
            return;
        }
        this.headerClaims.add(new StringClaim(claimName, value));
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
        JwtBuilder builder = new JwtBuilder();
        for (NamedClaim<?> payloadClaim : this.payloadClaims) {
            builder.withClaim(payloadClaim);
        }
        this.jwtString = builder.sign(this.algorithm, this.signingCredentials);
    }

    public void rebuild() {
        updatePayload(null);
    }

    public void setSigningCredentials(SigningCredentials signingCredentials) {
        this.signingCredentials = signingCredentials;
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
        return algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jwt)) return false;
        Jwt jwt = (Jwt) o;
        return Objects.equals(getHeaderClaims(), jwt.getHeaderClaims()) &&
                Objects.equals(getPayloadClaims(), jwt.getPayloadClaims()) &&
                Objects.equals(signingCredentials, jwt.signingCredentials) &&
                Objects.equals(jwtString, jwt.jwtString) &&
                Objects.equals(getAlgorithm(), jwt.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHeaderClaims(), getPayloadClaims(), signingCredentials, jwtString, getAlgorithm());
    }

    public boolean isEmpty() {
        return Jwt.EMPTY.equals(this);
    }
}
