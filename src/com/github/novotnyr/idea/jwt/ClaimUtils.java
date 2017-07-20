package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.Claim;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Arrays;
import java.util.List;

public class ClaimUtils {
    public static final List<String> NUMERIC_DATE_FIELDS = Arrays.asList("exp", "nbf", "iat");


    public static Object get(String claimName, Claim claimValue) {
        if(claimValue == null) {
            return null;
        }
        if(NUMERIC_DATE_FIELDS.contains(claimName)) {
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


}
