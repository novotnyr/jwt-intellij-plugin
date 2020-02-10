package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContext;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class RS256SignatureContext implements SignatureContext {
    private RSAPrivateKey privateKey;

    private RSAPublicKey publicKey;

    private RS256SignatureContext() {
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public boolean isEmpty() {
        return this.privateKey == null || this.publicKey == null;
    }

    public String getPrivateKeyString() {
        if (this.privateKey == null) {
            return "";
        }
        return RsaUtils.toString(this.privateKey);
    }

    public String getPublicKeyString() {
        return RsaUtils.toString(this.publicKey);
    }


    public static class Builder {
        private String publicKey;

        private String privateKey;

        private boolean sanitizingWhitespace = true;

        public Builder withPublicKey(final String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder withPrivateKey(final String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public RS256SignatureContext build() {
            RS256SignatureContext signatureContext = new RS256SignatureContext();
            signatureContext.publicKey = RsaUtils.getPublicKey(sanitizeWhitespace(this.publicKey));
            if (this.privateKey != null) {
                signatureContext.privateKey = RsaUtils.getPrivateKey(sanitizeWhitespace(this.privateKey));
            }
            return signatureContext;
        }

        private String sanitizeWhitespace(String publicKey) {
            if (!sanitizingWhitespace) {
                return publicKey;
            }
            return RsaUtils.sanitizeWhitespace(publicKey);
        }
    }
}
