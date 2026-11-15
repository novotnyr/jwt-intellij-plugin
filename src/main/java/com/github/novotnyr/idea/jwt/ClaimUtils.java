package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.novotnyr.idea.jwt.core.BooleanClaim;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.core.RawClaim;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry.DataType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ClaimUtils {

    /** The raw JSON value preserves precision for registered NumericDate claims. */
    public static NamedClaim<?> getClaim(String claimName, Claim claimValue, JsonNode rawValue) {
        if (isDateClaim(claimName, claimValue)) {
            BigDecimal timestamp = NumericDate.fromJson(rawValue);
            return timestamp == null
                    ? new RawClaim(claimName, rawValue)
                    : DateClaim.fromNumericDate(claimName, timestamp);
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
        return Configuration.INSTANCE.getTimestampDateFields().contains(claimName);
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
            if (value instanceof BigDecimal numericDate) {
                return DateClaim.fromNumericDate(name, numericDate);
            }
            if (value instanceof String text) {
                return DateClaim.fromNumericDate(name, new BigDecimal(text));
            }
            if (value instanceof Long seconds) {
                return DateClaim.fromNumericDate(name, BigDecimal.valueOf(seconds));
            }
            if (value instanceof Date date) {
                return new DateClaim(name, date);
            }
            throw new IllegalArgumentException("Unsupported timestamp value: " + value);
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
