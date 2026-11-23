package com.github.novotnyr.idea.jwt;

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

public class ClaimUtils {

    /** Converts a parsed JSON field to the claim type used by the payload editor. */
    public static NamedClaim<?> toPayloadClaim(String claimName, JsonNode value) {
        if (isDateClaim(claimName)) {
            BigDecimal timestamp = NumericDate.fromJson(value);
            if (timestamp != null) {
                return DateClaim.fromNumericDate(claimName, timestamp);
            }
            if (!value.isTextual()) {
                return new RawClaim(claimName, value.deepCopy());
            }
        }

        return switch (value.getNodeType()) {
            case BOOLEAN -> new BooleanClaim(claimName, value.booleanValue());
            case NUMBER -> new NumericClaim(claimName, value.decimalValue());
            case ARRAY, OBJECT -> new RawClaim(claimName, value.deepCopy());
            default -> new StringClaim(claimName, value.textValue());
        };
    }

    private static boolean isDateClaim(String claimName) {
        return Configuration.INSTANCE.getTimestampDateFields().contains(claimName);
    }

    public static NamedClaim<?> newEmptyClaim(DataType dataType) {
        return switch (dataType) {
            case BOOLEAN -> new BooleanClaim("claim", false);
            case STRING -> new StringClaim("claim", "value");
            case NUMERIC -> new NumericClaim("claim", 0L);
            case TIMESTAMP -> new DateClaim("claim", new Date());
            case RAW -> new RawClaim("claim", MissingNode.getInstance());
        };
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
            return new NumericClaim(name, (BigDecimal) value);
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
