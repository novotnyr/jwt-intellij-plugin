package com.github.novotnyr.idea.jwt.core;

import java.util.Objects;

public class StringSecret implements SigningCredentials {
    private final String secret;

    public StringSecret(String secret) {
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringSecret)) return false;
        StringSecret that = (StringSecret) o;
        return Objects.equals(getSecret(), that.getSecret());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSecret());
    }
}

