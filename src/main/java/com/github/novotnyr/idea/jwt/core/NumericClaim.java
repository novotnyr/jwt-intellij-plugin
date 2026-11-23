package com.github.novotnyr.idea.jwt.core;

import java.math.BigDecimal;

public class NumericClaim extends NamedClaim<BigDecimal> {
    public NumericClaim(String name, Long value) {
        super(name, value == null ? null : BigDecimal.valueOf(value));
    }

    public NumericClaim(String name, BigDecimal value) {
        super(name, value);
    }

    @Override
    public NumericClaim copy() {
        return new NumericClaim(this.getName(), this.getValue());
    }
}
