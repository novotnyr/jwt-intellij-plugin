package com.github.novotnyr.idea.jwt.validation;

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
        return new JwtErrors(this.claimsValidator.validate(jwt.getDecodedJwt()), null);
    }

    public JwtErrors validate(Jwt jwt, SignatureContext signatureContext) {
        return validate(jwt.getDecodedJwt(), signatureContext);
    }

    public JwtErrors validate(DecodedJWT jwt, SignatureContext signatureContext) {
        List<ClaimError> claimErrors = this.claimsValidator.validate(jwt);
        SignatureError signatureError = this.signatureValidator.validate(jwt, signatureContext).orElse(null);
        return new JwtErrors(claimErrors, signatureError);
    }
}
