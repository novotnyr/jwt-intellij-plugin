package com.github.novotnyr.idea.jwt.core;

import java.math.BigDecimal;
import java.util.Date;

public class DateClaim extends NamedClaim<BigDecimal> {
    public DateClaim(String name, Date value) {
        super(name, value == null ? null : BigDecimal.valueOf(Math.floorDiv(value.getTime(), 1000)));
    }

    private DateClaim(String name, BigDecimal value) {
        super(name, value);
    }

    /** Keeps the original numeric seconds, including any fractional part. */
    public static DateClaim fromNumericDate(String name, BigDecimal value) {
        return new DateClaim(name, value);
    }

    /** Returns a millisecond-precision value for display, or null outside {@link Date}'s range. */
    public Date getDate() {
        return NumericDate.toDate(getValue());
    }

    @Override
    public String getValueString() {
        if(getValue() == null) {
            return "null";
        } else {
            return getValue().toString();
        }
    }

    @Override
    public DateClaim copy() {
        return new DateClaim(getName(), getValue());
    }
}
