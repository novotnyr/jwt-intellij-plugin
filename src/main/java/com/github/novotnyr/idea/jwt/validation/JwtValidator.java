package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class JwtValidator {
    public final Logger logger = LoggerFactory.getLogger(getClass());

    private final JwtSignatureValidator signatureValidator = new JwtSignatureValidator();

    private final JwtClaimsValidator claimsValidator;

    private SignatureError signatureError;

    private final List<ClaimError> claimErrors = new ArrayList<>();

    public JwtValidator() {
        this(Clock.systemUTC());
    }

    JwtValidator(Clock clock) {
        this.claimsValidator = new JwtClaimsValidator(clock);
    }

    public JwtValidator validateClaims(Jwt jwt) {
        DecodedJWT decodedJWT = JWT.decode(jwt.toString());
        return validateClaims(decodedJWT);
    }

    private JwtValidator validateClaims(DecodedJWT jwt) {
        this.claimErrors.addAll(this.claimsValidator.validate(jwt));
        return this;
    }

    public void validate(Jwt jwt, SignatureContext signatureContext) {
        DecodedJWT decodedJWT = JWT.decode(jwt.toString());
        validate(decodedJWT, signatureContext);
    }

    private void validate(DecodedJWT jwt, SignatureContext signatureContext) {
        validateClaims(jwt);
        this.signatureValidator.validate(jwt, signatureContext)
                .filter(ignored -> this.signatureError == null)
                .ifPresent(error -> this.signatureError = error);
    }

    public SignatureError getSignatureError() {
        return this.signatureError;
    }

    public boolean hasSignatureError() {
        return this.signatureError != null;
    }

    public List<ClaimError> getClaimErrors() {
        return this.claimErrors;
    }
}
