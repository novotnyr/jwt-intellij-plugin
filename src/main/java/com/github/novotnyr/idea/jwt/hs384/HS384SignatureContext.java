package com.github.novotnyr.idea.jwt.hs384;

import com.github.novotnyr.idea.jwt.SignatureContext;

public record HS384SignatureContext(String secret) implements SignatureContext {
    @Override
    public boolean isEmpty() {
        return this.secret == null || this.secret.isEmpty();
    }
}
