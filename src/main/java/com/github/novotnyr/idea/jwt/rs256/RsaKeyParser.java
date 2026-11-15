package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContextException;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static com.github.novotnyr.idea.jwt.rs256.RsaKeyValidationException.Field.PRIVATE;
import static com.github.novotnyr.idea.jwt.rs256.RsaKeyValidationException.Field.PUBLIC;

public final class RsaKeyParser {
    private RsaKeyParser() {
    }

    /**
     * Parses the two RSA fields independently, including literal newline escapes from pasted keys.
     *
     * @throws RsaKeyValidationException if either field contains an invalid key; the exception identifies the field
     */
    public static RS256SignatureContext parse(String publicKeyText, String privateKeyText) {
        RSAPublicKey publicKey = null;
        RSAPrivateKey privateKey = null;
        if (publicKeyText != null && !publicKeyText.isEmpty()) {
            try {
                publicKey = RsaUtils.getPublicKey(RsaUtils.sanitizeWhitespace(publicKeyText));
            } catch (SignatureContextException e) {
                throw new RsaKeyValidationException(PUBLIC, e);
            }
        }
        if (privateKeyText != null && !privateKeyText.isEmpty()) {
            try {
                privateKey = RsaUtils.getPrivateKey(RsaUtils.sanitizeWhitespace(privateKeyText));
            } catch (SignatureContextException e) {
                throw new RsaKeyValidationException(PRIVATE, e);
            }
        }
        return RS256SignatureContext.fromKeys(publicKey, privateKey);
    }
}
