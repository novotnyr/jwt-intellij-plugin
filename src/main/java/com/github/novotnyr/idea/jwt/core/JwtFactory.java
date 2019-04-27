package com.github.novotnyr.idea.jwt.core;

import com.github.novotnyr.idea.jwt.SignatureContext;

import java.util.Date;

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
        jwt.setHeaderClaim(new StringClaim("alg", algorithm));
        jwt.setHeaderClaim(new StringClaim("typ", "JWT"));
        if (addIat) {
            jwt.setPayloadClaim(new DateClaim("iat", new Date()));
        }
        jwt.rebuild();
        return jwt;
    }
}
