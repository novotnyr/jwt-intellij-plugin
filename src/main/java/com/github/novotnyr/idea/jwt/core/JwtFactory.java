package com.github.novotnyr.idea.jwt.core;

import com.github.novotnyr.idea.jwt.SignatureContext;

import java.util.Date;

import static com.auth0.jwt.HeaderParams.ALGORITHM;
import static com.auth0.jwt.HeaderParams.TYPE;
import static com.auth0.jwt.RegisteredClaims.ISSUED_AT;

public class JwtFactory {
    private static JwtFactory INSTANCE = new JwtFactory();

    public static JwtFactory getInstance() {
        return INSTANCE;
    }

    private JwtFactory() {
        // empty constructor
    }

    public Jwt newJwt(String algorithm, SignatureContext signatureContext, boolean addIat) {
        Jwt jwt = new Jwt();
        jwt.setAlgorithm(algorithm);
        jwt.setSignatureContext(signatureContext);
        jwt.setHeaderClaim(new StringClaim(ALGORITHM, algorithm));
        jwt.setHeaderClaim(new StringClaim(TYPE, "JWT"));
        if (addIat) {
            jwt.setPayloadClaim(new DateClaim(ISSUED_AT, new Date()));
        }
        jwt.rebuild();
        return jwt;
    }
}
