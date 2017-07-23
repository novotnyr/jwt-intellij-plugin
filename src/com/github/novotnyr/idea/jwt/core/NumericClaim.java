package com.github.novotnyr.idea.jwt.core;

public class NumericClaim extends NamedClaim<Long> {
    public NumericClaim(String name, Long value) {
        super(name, value);
    }

    @Override
    public NumericClaim copy() {
        return new NumericClaim(this.getName(), this.getValue());
    }
}
