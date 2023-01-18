package com.github.novotnyr.idea.jwt.hs384;

import com.github.novotnyr.idea.jwt.SignatureContext;

import java.util.Objects;

public class HS384SignatureContext implements SignatureContext {
    private final String secret;

    public HS384SignatureContext(String secret) {
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public boolean isEmpty() {
        return this.secret == null || this.secret.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HS384SignatureContext)) return false;
        HS384SignatureContext that = (HS384SignatureContext) o;
        return Objects.equals(getSecret(), that.getSecret());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSecret());
    }
}
