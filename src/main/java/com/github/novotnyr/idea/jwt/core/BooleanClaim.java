package com.github.novotnyr.idea.jwt.core;

public class BooleanClaim extends NamedClaim<Boolean> {
    public BooleanClaim(String name, Boolean value) {
        super(name, value);
    }

    @Override
    public BooleanClaim copy() {
        return new BooleanClaim(getName(), getValue());
    }
}
