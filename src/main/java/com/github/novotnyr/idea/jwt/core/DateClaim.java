package com.github.novotnyr.idea.jwt.core;

import java.util.Date;

public class DateClaim extends NamedClaim<Date> {
    public DateClaim(String name, Date value) {
        super(name, value);
    }

    @Override
    public String getValueString() {
        if(getValue() == null) {
            return "null";
        } else {
            return String.valueOf(this.getValue().getTime() / 1000);
        }
    }

    @Override
    public DateClaim copy() {
        return new DateClaim(getName(), getValue());
    }
}
