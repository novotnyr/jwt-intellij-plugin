package com.github.novotnyr.idea.jwt.hs256;

import com.github.novotnyr.idea.jwt.SignatureContext;

public record HS256SignatureContext(String secret) implements SignatureContext {
    @Override
    public boolean isEmpty() {
        return this.secret == null || this.secret.isEmpty();
    }
}
