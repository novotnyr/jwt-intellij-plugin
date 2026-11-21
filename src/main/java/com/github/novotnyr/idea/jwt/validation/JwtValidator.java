package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;

import java.time.Clock;
import java.util.List;

public class JwtValidator {
    private final JwtSignatureValidator signatureValidator = new JwtSignatureValidator();

    private final JwtClaimsValidator claimsValidator;

    public JwtValidator() {
        this(Clock.systemUTC());
    }

    JwtValidator(Clock clock) {
        this.claimsValidator = new JwtClaimsValidator(clock);
    }

    public JwtErrors validateClaims(Jwt jwt) {
        DecodedJWT decodedJWT = JWT.decode(jwt.toString());
        return new JwtErrors(this.claimsValidator.validate(decodedJWT), null);
    }

    public JwtErrors validate(Jwt jwt, SignatureContext signatureContext) {
        DecodedJWT decodedJWT = JWT.decode(jwt.toString());
        return validate(decodedJWT, signatureContext);
    }

    public JwtErrors validate(DecodedJWT jwt, SignatureContext signatureContext) {
        List<ClaimError> claimErrors = this.claimsValidator.validate(jwt);
        SignatureError signatureError = this.signatureValidator.validate(jwt, signatureContext).orElse(null);
        return new JwtErrors(claimErrors, signatureError);
    }
}
