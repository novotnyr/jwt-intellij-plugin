package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Payload;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.core.NumericDate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JwtClaimsValidator {
    private final Clock clock;

    public JwtClaimsValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    /** Returns the timestamp claim errors for the decoded JWT. */
    public List<ClaimError> validate(DecodedJWT jwt) {
        List<ClaimError> claimErrors = new ArrayList<>();
        BigDecimal now = NumericDate.fromInstant(this.clock.instant());

        validateExpiration(jwt, now, claimErrors);
        validateNotBefore(jwt, now, claimErrors);
        validateIssuedAt(jwt, claimErrors);

        return List.copyOf(claimErrors);
    }

    private void validateExpiration(Payload payload, BigDecimal now, List<ClaimError> claimErrors) {
        BigDecimal expiration = getNumericDateOrReportError(payload, RegisteredClaims.EXPIRES_AT, claimErrors);
        if (expiration != null && now.compareTo(expiration) >= 0) {
            claimErrors.add(new ClaimError(RegisteredClaims.EXPIRES_AT, expirationMessage(expiration)));
        }
    }

    public static String expirationMessage(BigDecimal expiration) {
        Instant instant = NumericDate.toInstant(expiration);
        return instant == null ? "Token expired" : "Token expired at " + instant;
    }

    private void validateNotBefore(Payload payload, BigDecimal now, List<ClaimError> claimErrors) {
        BigDecimal notBefore = getNumericDateOrReportError(payload, RegisteredClaims.NOT_BEFORE, claimErrors);
        if (notBefore != null && now.compareTo(notBefore) < 0) {
            claimErrors.add(new ClaimError(RegisteredClaims.NOT_BEFORE, "Not-before time is in the future"));
        }
    }

    private void validateIssuedAt(Payload payload, List<ClaimError> claimErrors) {
        // RFC 7519 defines iat as an issue time, without requiring it to precede now.
        getNumericDateOrReportError(payload, RegisteredClaims.ISSUED_AT, claimErrors);
    }

    private BigDecimal getNumericDateOrReportError(Payload payload, String claimName, List<ClaimError> claimErrors) {
        Claim claim = payload.getClaim(claimName);
        if (claim.isMissing()) {
            return null;
        }
        BigDecimal timestamp = NumericDate.fromJson(claim.as(JsonNode.class));
        if (timestamp == null) {
            claimErrors.add(new ClaimError(claimName, "Illegal type: NumericDate must be a JSON number"));
        }
        return timestamp;
    }

}
