package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.github.novotnyr.idea.jwt.core.BooleanClaim;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ClaimUtils {
    public static final List<String> NUMERIC_DATE_FIELDS = Arrays.asList("exp", "nbf", "iat");


    public static Object get(String claimName, Claim claimValue) {
        return get(claimName, claimValue, true);
    }

    public static Object get(String claimName, Claim claimValue, boolean prettify) {
        if(claimValue == null) {
            return null;
        }
        if(prettify && NUMERIC_DATE_FIELDS.contains(claimName)) {
            if (Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.ISO) {
                return claimValue.asDate();
            }
            if (Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RELATIVE) {
                PrettyTime prettyTime = new PrettyTime();
                return prettyTime.format(claimValue.asDate());
            }
        }

        Boolean booleanClaim = claimValue.asBoolean();
        if(booleanClaim != null) {
            return booleanClaim;
        }
        Long longClaim = claimValue.asLong();
        if(longClaim != null) {
            return longClaim;
        }
        return claimValue.asString();
    }

    public static NamedClaim<?> getClaim(String claimName, Claim claimValue) {
        Date date = claimValue.asDate();
        if(date != null) {
            return new DateClaim(claimName, date);
        }
        Boolean bool = claimValue.asBoolean();
        if(bool != null) {
            return new BooleanClaim(claimName, bool);
        }
        Long longClaim = claimValue.asLong();
        if(longClaim != null) {
            return new NumericClaim(claimName, longClaim);
        }
        return new StringClaim(claimName, claimValue.asString());

    }

    public static NamedClaim<?> newClaim(String name, Object value) {
        if(value instanceof Date) {
            return new DateClaim(name, (Date) value);
        }
        if(value instanceof Boolean) {
            return new BooleanClaim(name, (Boolean) value);
        }
        if(value instanceof Long) {
            return new NumericClaim(name, (Long) value);
        }
        if(value instanceof String) {
            return new StringClaim(name, (String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to claim value");
    }
}
