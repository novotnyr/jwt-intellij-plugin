package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
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
        DecodedJWT decodedJwt = JWT.decode(encodedJwt);

        headerClaims.add(new StringClaim("alg", decodedJwt.getAlgorithm()));
        headerClaims.add(new StringClaim("typ", decodedJwt.getType()));
        headerClaims.add(new StringClaim("cty", decodedJwt.getContentType()));
        headerClaims.add(new StringClaim("kid", decodedJwt.getKeyId()));

        this.algorithm = decodedJwt.getAlgorithm();

        for (Map.Entry<String, Claim> claim : decodedJwt.getClaims().entrySet()) {
            this.payloadClaims.add(ClaimUtils.getClaim(claim.getKey(), claim.getValue()));
        }
    }

    public List<NamedClaim<?>> getHeaderClaims() {
        return Collections.unmodifiableList(this.getHeaderClaims());
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
            if(claim instanceof StringClaim) {
                builder.withClaim(claim.getName(), (String) claim.getValue());
            }
            if(claim instanceof NumericClaim) {
                builder.withClaim(claim.getName(), (Long) claim.getValue());
            }
            if(claim instanceof DateClaim) {
                builder.withClaim(claim.getName(), (Date) claim.getValue());
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

    public String toString() {
        return this.jwtString;
    }
}
