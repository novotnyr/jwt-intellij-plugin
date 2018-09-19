package com.github.novotnyr.idea.jwt.core;

import java.util.Date;

public class JwtFactory {
    private static JwtFactory INSTANCE = new JwtFactory();

    public static JwtFactory getInstance() {
        return INSTANCE;
    }

    private JwtFactory() {
        // empty constructor
    }

    public Jwt newJwt(String algorithm, SigningCredentials signingCredentials, boolean addIat) {
        Jwt jwt = new Jwt();
        jwt.setAlgorithm(algorithm);
        jwt.setSigningCredentials(signingCredentials);
        jwt.setHeaderClaim(new StringClaim("alg", "HS256"));
        jwt.setHeaderClaim(new StringClaim("typ", "JWT"));
        if (addIat) {
            jwt.setPayloadClaim(new DateClaim("iat", new Date()));
        }
        jwt.rebuild();
        return jwt;
    }
}
