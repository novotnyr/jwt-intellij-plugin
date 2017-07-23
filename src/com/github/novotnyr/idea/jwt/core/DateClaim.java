package com.github.novotnyr.idea.jwt.core;

import java.util.Date;

public class DateClaim extends NamedClaim<Date> {
    public DateClaim(String name, Date value) {
        super(name, value);
    }
}
