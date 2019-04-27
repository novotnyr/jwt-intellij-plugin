package com.github.novotnyr.idea.jwt.hs256;

import com.github.novotnyr.idea.jwt.SignatureContext;

import java.util.Objects;

public class HS256SignatureContext implements SignatureContext {
    private final String secret;

    public HS256SignatureContext(String secret) {
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
        if (!(o instanceof HS256SignatureContext)) return false;
        HS256SignatureContext that = (HS256SignatureContext) o;
        return Objects.equals(getSecret(), that.getSecret());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSecret());
    }
}
