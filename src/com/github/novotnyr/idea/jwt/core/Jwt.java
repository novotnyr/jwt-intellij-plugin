package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.ClaimUtils;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Jwt {
    private DecodedJWT jwtImpl;

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

        for (Map.Entry<String, Claim> claim : decodedJwt.getClaims().entrySet()) {
            this.payloadClaims.add(ClaimUtils.getClaim(claim.getKey(), claim.getValue()));
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

    public void setPayloadClaim(NamedClaim<?> claim) {
        int i = 0;
        boolean found = false;
        for (NamedClaim<?> payloadClaim : payloadClaims) {
            if(payloadClaim.getName().equals(claim.getName())) {
                found = true;
                break;
            }
            i++;
        }
        if(found) {
            payloadClaims.set(i, claim);
        } else {
            payloadClaims.add(claim);
        }
        updatePayload(claim);
    }

    private void updatePayload(NamedClaim<?> claim) {
        JWTCreator.Builder builder = JWT.create();
        for (NamedClaim<?> payloadClaim : this.payloadClaims) {
            if(payloadClaim instanceof StringClaim) {
                builder.withClaim(payloadClaim.getName(), (String) payloadClaim.getValue());
            }
            if(payloadClaim instanceof NumericClaim) {
                builder.withClaim(payloadClaim.getName(), (Long) payloadClaim.getValue());
            }
            if(payloadClaim instanceof DateClaim) {
                builder.withClaim(payloadClaim.getName(), (Date) payloadClaim.getValue());
            }
        }
        this.jwtString = builder.sign(AlgoritmResolver.resolve(this.algorithm, this.signingCredentials));
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

}
