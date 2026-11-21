package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.JsonWebToken;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtDecoder;

import java.time.Clock;
import java.util.List;

public class JwtValidator {
    private final JwtSignatureValidator signatureValidator = new JwtSignatureValidator();

    private final JwtClaimsValidator claimsValidator;

    private final JwtDecoder decoder;

    public JwtValidator() {
        this(Clock.systemUTC());
    }

    JwtValidator(Clock clock) {
        this.claimsValidator = new JwtClaimsValidator(clock);
        this.decoder = new JwtDecoder(clock);
    }

    public JwtErrors validateClaims(Jwt jwt) {
        return this.decoder.decode(jwt.toString()).errors();
    }

    public JwtErrors validate(Jwt jwt, SignatureContext signatureContext) {
        JsonWebToken token = this.decoder.decode(jwt.toString());
        return validateSignature(token.decodedJwt(), signatureContext, token.errors().claimErrors());
    }

    public JwtErrors validate(DecodedJWT jwt, SignatureContext signatureContext) {
        return validateSignature(jwt, signatureContext, this.claimsValidator.validate(jwt));
    }

    private JwtErrors validateSignature(DecodedJWT jwt, SignatureContext signatureContext,
                                        List<ClaimError> claimErrors) {
        SignatureError signatureError = this.signatureValidator.validate(jwt, signatureContext).orElse(null);
        return new JwtErrors(claimErrors, signatureError);
    }
}
