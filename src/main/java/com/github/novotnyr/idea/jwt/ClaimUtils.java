package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.novotnyr.idea.jwt.core.BooleanClaim;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.github.novotnyr.idea.jwt.core.RawClaim;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry.DataType;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ClaimUtils {

    public static NamedClaim<?> getClaim(String claimName, Claim claimValue) {
        Date date = claimValue.asDate();
        if (isDateClaim(claimName, claimValue)) {
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
        List<Object> objects = claimValue.asList(Object.class);
        if (objects != null) {
            return new RawClaim(claimName, claimValue);
        }
        Map<String, Object> map = claimValue.asMap();
        if (map != null) {
            return new RawClaim(claimName, claimValue);
        }

        return new StringClaim(claimName, claimValue.asString());

    }

    public static boolean isDateClaim(String claimName, Claim claimValue) {
        return claimValue.getClass() != null
                && Configuration.INSTANCE.getTimestampDateFields().contains(claimName);
    }

    public static NamedClaim<?> newEmptyClaim(DataType dataType) {
        switch (dataType) {
            case BOOLEAN:
                return new BooleanClaim("claim", false);
            case STRING:
                return new StringClaim("claim", "value");
            case NUMERIC:
                return new NumericClaim("claim", 0L);
            case TIMESTAMP:
                return new DateClaim("claim", new Date());
            case RAW:
                return new RawClaim("claim", MissingNode.getInstance());
            default:
                throw new IllegalStateException("Unknown data type " + dataType);
        }
    }

    public static NamedClaim<?> copyClaim(NamedClaim<?> templateClaim, String name, Object value) {
        if(templateClaim instanceof DateClaim) {
            Date date = null;
            if(value == null) {
                date = null;
            }
            if(value instanceof String) {
                date = DateUtils.toDate((String) value);
            } else if (value instanceof Long) {
                date = new Date((Long) value * 1000);
            } else if (value instanceof Date) {
                date = (Date) value;
            }
            return new DateClaim(name, date);
        }
        if(templateClaim instanceof BooleanClaim) {
            return new BooleanClaim(name, (Boolean) value);
        }
        if(templateClaim instanceof NumericClaim) {
            return new NumericClaim(name, (Long) value);
        }
        if(templateClaim instanceof StringClaim) {
            return new StringClaim(name, (String) value);
        }
        if(templateClaim instanceof RawClaim) {
            return new RawClaim(name, (TreeNode) value);
        }

        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to claim value");
    }

}
