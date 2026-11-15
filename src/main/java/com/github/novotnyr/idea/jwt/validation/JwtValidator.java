package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.AlgorithmResolver;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JwtValidator {
    public final Logger logger = LoggerFactory.getLogger(getClass());

    private final AlgorithmResolver algorithmResolver = new AlgorithmResolver();

    private final Clock clock;

    private final List<GlobalError> globalErrors = new ArrayList<>();

    private final List<ClaimError> claimErrors = new ArrayList<>();

    public JwtValidator() {
        this(Clock.systemUTC());
    }

    /** Allows temporal validation to use one controlled clock in boundary tests. */
    JwtValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public JwtValidator validateClaims(Jwt jwt) {
        DecodedJWT decodedJWT = JWT.decode(jwt.toString());
        return validateClaims(decodedJWT);
    }

    private JwtValidator validateClaims(DecodedJWT jwt) {
        if (!hasTimestampClaims(jwt)) {
            return this;
        }

        JsonNode payload = NumericDate.parsePayload(jwt);
        BigDecimal now = NumericDate.fromInstant(this.clock.instant());

        validateExpiration(payload, now);
        validateNotBefore(payload, now);
        validateIssuedAt(payload);

        return this;
    }

    private boolean hasTimestampClaims(DecodedJWT jwt) {
        return !jwt.getClaim(RegisteredClaims.EXPIRES_AT).isMissing()
                || !jwt.getClaim(RegisteredClaims.NOT_BEFORE).isMissing()
                || !jwt.getClaim(RegisteredClaims.ISSUED_AT).isMissing();
    }

    private void validateExpiration(JsonNode payload, BigDecimal now) {
        BigDecimal expiration = getNumericDateOrReportError(payload, RegisteredClaims.EXPIRES_AT);
        if (expiration != null && now.compareTo(expiration) >= 0) {
            this.claimErrors.add(new ClaimError(RegisteredClaims.EXPIRES_AT, "Expiration time has passed"));
        }
    }

    private void validateNotBefore(JsonNode payload, BigDecimal now) {
        BigDecimal notBefore = getNumericDateOrReportError(payload, RegisteredClaims.NOT_BEFORE);
        if (notBefore != null && now.compareTo(notBefore) < 0) {
            this.claimErrors.add(new ClaimError(RegisteredClaims.NOT_BEFORE, "Not-before time is in the future"));
        }
    }

    private void validateIssuedAt(JsonNode payload) {
        // RFC 7519 defines iat as an issue time, without requiring it to precede now.
        getNumericDateOrReportError(payload, RegisteredClaims.ISSUED_AT);
    }

    public void validate(Jwt jwt, SignatureContext signatureContext) {
        DecodedJWT decodedJWT = JWT.decode(jwt.toString());
        validate(decodedJWT, signatureContext);
    }

    private void validate(DecodedJWT jwt, SignatureContext signatureContext) {
        validateClaims(jwt);
        try {
            Algorithm algorithm = this.algorithmResolver.resolve(jwt.getAlgorithm(), signatureContext);
            algorithm.verify(jwt);
        } catch (SignatureVerificationException e) {
            this.globalErrors.add(new SignatureError());
        } catch (IllegalArgumentException e) {
            this.globalErrors.add(SignatureError.forEmptySecret());
        } catch (UnknownAlgorithmException e) {
            this.globalErrors.add(SignatureError.forUnknownAlgorithm(jwt.getAlgorithm()));
        }
    }

    public SignatureError getSignatureError() {
        for (GlobalError globalError : this.globalErrors) {
            if(globalError instanceof SignatureError) {
                return (SignatureError) globalError;
            }
        }
        return null;
    }

    public boolean hasSignatureError() {
        for (GlobalError globalError : this.globalErrors) {
            if(globalError instanceof SignatureError) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal getNumericDateOrReportError(JsonNode payload, String claimName) {
        JsonNode claim = payload.get(claimName);
        if (claim == null) {
            return null;
        }
        BigDecimal timestamp = NumericDate.fromJson(claim);
        if (timestamp == null) {
            this.claimErrors.add(new ClaimError(claimName, "Illegal type: NumericDate must be a JSON number"));
        }
        return timestamp;
    }

    public List<ClaimError> getClaimErrors() {
        return this.claimErrors;
    }

    public List<GlobalError> getGlobalErrors() {
        return this.globalErrors;
    }
}
