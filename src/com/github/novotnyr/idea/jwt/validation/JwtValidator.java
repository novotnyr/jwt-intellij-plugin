package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.impl.PublicClaims;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JwtValidator {
    public final Logger logger = LoggerFactory.getLogger(getClass());

    private List<GlobalError> globalErrors = new ArrayList<>();

    private List<ClaimError> claimErrors = new ArrayList<>();

    public JwtValidator validateClaims(DecodedJWT jwt) {
        assertInFuture(jwt, PublicClaims.EXPIRES_AT);
        assertInPast(jwt, PublicClaims.ISSUED_AT);
        assertInPast(jwt, PublicClaims.NOT_BEFORE);

        return this;
    }

    public void validate(DecodedJWT jwt, Object validationContext) {
        validateClaims(jwt);

        String algorithmString = jwt.getAlgorithm();
        Algorithm algorithm = null;
        switch (algorithmString) {
            case "HS256" : {
                if (validationContext instanceof byte[]) {
                    byte[] byteArraySecret = (byte[]) validationContext;
                    algorithm = Algorithm.HMAC256(byteArraySecret);
                } else if (validationContext instanceof String) {
                    try {
                        String secret = (String) validationContext;
                        algorithm = Algorithm.HMAC256(secret);
                    } catch (UnsupportedEncodingException e) {
                        // UTF-8 should be supported everywhere on JVM, so this
                        // won't happen
                        logger.error("Unsupported encoding", e);
                        throw new UnknownAlgorithmException(algorithmString);
                    }
                }
                break;
            }
            default:
                globalErrors.add(SignatureError.forUnknownAlgorithm(algorithmString));
                return;
        }
        try {
            algorithm.verify(jwt);
        } catch (SignatureVerificationException e) {
            globalErrors.add(new SignatureError());
        } catch (IllegalArgumentException e) {
            globalErrors.add(SignatureError.forEmptySecret());
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

    private void assertInFuture(DecodedJWT jwt, String claimName) {
        Date date = jwt.getClaim(claimName).asDate();
        if(date == null) {
            this.claimErrors.add(new ClaimError(claimName, "Illegal type: not a date"));
            return;
        }
        if(!date.after(new Date())) {
            this.claimErrors.add(new ClaimError(claimName, "Future date expected"));
        }
    }

    private void assertInPast(DecodedJWT jwt, String claimName) {
        Date date = jwt.getClaim(claimName).asDate();
        if(date == null) {
            this.claimErrors.add(new ClaimError(claimName, "Illegal type: not a date"));
            return;
        }
        if(!date.before(new Date())) {
            this.claimErrors.add(new ClaimError(claimName, "Past date expected"));
        }
    }

    public List<ClaimError> getClaimErrors() {
        return claimErrors;
    }

    public List<GlobalError> getGlobalErrors() {
        return globalErrors;
    }
}
