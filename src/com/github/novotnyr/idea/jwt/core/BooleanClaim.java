package com.github.novotnyr.idea.jwt.core;

public class BooleanClaim extends NamedClaim<Boolean> {
    public BooleanClaim(String name, Boolean value) {
        super(name, value);
    }
}
