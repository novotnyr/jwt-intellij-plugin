package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.core.NumericDate;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JwtClaimsValidator {
    private final Clock clock;

    public JwtClaimsValidator() {
        this(Clock.systemUTC());
    }

    JwtClaimsValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    /** Returns the timestamp claim errors for the decoded JWT. */
    public List<ClaimError> validate(DecodedJWT jwt) {
        List<ClaimError> claimErrors = new ArrayList<>();
        if (!hasTimestampClaims(jwt)) {
            return claimErrors;
        }

        JsonNode payload = NumericDate.parsePayload(jwt);
        BigDecimal now = NumericDate.fromInstant(this.clock.instant());

        validateExpiration(payload, now, claimErrors);
        validateNotBefore(payload, now, claimErrors);
        validateIssuedAt(payload, claimErrors);

        return claimErrors;
    }

    private boolean hasTimestampClaims(DecodedJWT jwt) {
        return !jwt.getClaim(RegisteredClaims.EXPIRES_AT).isMissing()
                || !jwt.getClaim(RegisteredClaims.NOT_BEFORE).isMissing()
                || !jwt.getClaim(RegisteredClaims.ISSUED_AT).isMissing();
    }

    private void validateExpiration(JsonNode payload, BigDecimal now, List<ClaimError> claimErrors) {
        BigDecimal expiration = getNumericDateOrReportError(payload, RegisteredClaims.EXPIRES_AT, claimErrors);
        if (expiration != null && now.compareTo(expiration) >= 0) {
            claimErrors.add(new ClaimError(RegisteredClaims.EXPIRES_AT, "Expiration time has passed"));
        }
    }

    private void validateNotBefore(JsonNode payload, BigDecimal now, List<ClaimError> claimErrors) {
        BigDecimal notBefore = getNumericDateOrReportError(payload, RegisteredClaims.NOT_BEFORE, claimErrors);
        if (notBefore != null && now.compareTo(notBefore) < 0) {
            claimErrors.add(new ClaimError(RegisteredClaims.NOT_BEFORE, "Not-before time is in the future"));
        }
    }

    private void validateIssuedAt(JsonNode payload, List<ClaimError> claimErrors) {
        // RFC 7519 defines iat as an issue time, without requiring it to precede now.
        getNumericDateOrReportError(payload, RegisteredClaims.ISSUED_AT, claimErrors);
    }

    private BigDecimal getNumericDateOrReportError(JsonNode payload, String claimName, List<ClaimError> claimErrors) {
        JsonNode claim = payload.get(claimName);
        if (claim == null) {
            return null;
        }
        BigDecimal timestamp = NumericDate.fromJson(claim);
        if (timestamp == null) {
            claimErrors.add(new ClaimError(claimName, "Illegal type: NumericDate must be a JSON number"));
        }
        return timestamp;
    }
}
