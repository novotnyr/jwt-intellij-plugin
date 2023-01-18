package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.algorithms.Algorithm;
import com.github.novotnyr.idea.jwt.SecretNotSpecifiedException;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.hs384.HS384SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.validation.UnknownAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class AlgorithmResolver {
    public static final Logger logger = LoggerFactory.getLogger(Algorithm.class);

    public Algorithm resolve(String algorithmName, SignatureContext signatureContext) {
        Algorithm algorithm = null;
        switch (algorithmName) {
            case SignatureAlgorithm.HS256: {
                if(signatureContext == null) {
                    throw new SecretNotSpecifiedException();
                } else if (signatureContext instanceof HS256SignatureContext) {
                    try {
                        String secret = ((HS256SignatureContext) signatureContext).getSecret();
                        algorithm = Algorithm.HMAC256(secret);
                    } catch (UnsupportedEncodingException e) {
                        // UTF-8 should be supported everywhere on JVM, so this
                        // won't happen
                        logger.error("Unsupported encoding", e);
                        throw new UnknownAlgorithmException(algorithmName);
                    }
                }
                break;
            }
            case SignatureAlgorithm.HS384: {
                if(signatureContext == null) {
                    throw new SecretNotSpecifiedException();
                } else if (signatureContext instanceof HS384SignatureContext) {
                    try {
                        String secret = ((HS384SignatureContext) signatureContext).getSecret();
                        algorithm = Algorithm.HMAC384(secret);
                    } catch (UnsupportedEncodingException e) {
                        // UTF-8 should be supported everywhere on JVM, so this
                        // won't happen
                        logger.error("Unsupported encoding", e);
                        throw new UnknownAlgorithmException(algorithmName);
                    }
                }
                break;
            }
            case SignatureAlgorithm.RS256: {
                if(signatureContext == null) {
                    throw new SecretNotSpecifiedException();
                } else if (signatureContext instanceof RS256SignatureContext) {
                    RS256SignatureContext keyPairContext = (RS256SignatureContext) signatureContext;
                    algorithm = Algorithm.RSA256(keyPairContext.getPublicKey(), keyPairContext.getPrivateKey());
                }
                break;
            }
            default:
                throw new UnknownAlgorithmException(algorithmName);
        }
        return algorithm;
    }
}
