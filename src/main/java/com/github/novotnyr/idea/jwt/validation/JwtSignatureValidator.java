package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.AlgorithmResolver;

import java.util.Optional;

public class JwtSignatureValidator {
    private final AlgorithmResolver algorithmResolver = new AlgorithmResolver();

    /**
     * Returns an empty result if the signature is valid.
     */
    public Optional<SignatureError> validate(DecodedJWT jwt, SignatureContext signatureContext) {
        try {
            Algorithm algorithm = this.algorithmResolver.resolve(jwt.getAlgorithm(), signatureContext);
            algorithm.verify(jwt);
            return Optional.empty();
        } catch (SignatureVerificationException e) {
            return Optional.of(new SignatureError());
        } catch (IllegalArgumentException e) {
            return Optional.of(SignatureError.forEmptySecret());
        } catch (UnknownAlgorithmException e) {
            return Optional.of(SignatureError.forUnknownAlgorithm(jwt.getAlgorithm()));
        }
    }
}
