package com.github.novotnyr.idea.jwt.core;

public class StringSecret implements SigningCredentials {
    private final String secret;

    public StringSecret(String secret) {
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }
}

