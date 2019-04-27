package com.github.novotnyr.idea.jwt.core;

public class StringClaim extends NamedClaim<String> {
    public StringClaim(String name, String value) {
        super(name, value);
    }

    @Override
    public StringClaim copy() {
        return new StringClaim(this.getName(), this.getValue());
    }
}
