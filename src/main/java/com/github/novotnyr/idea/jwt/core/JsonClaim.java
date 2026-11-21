package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.novotnyr.idea.jwt.core.JwtJson.MAPPER;

/** Exposes a parsed JSON value through Auth0's claim interface. */
record JsonClaim(JsonNode value) implements Claim {
    @Override
    public boolean isNull() {
        return this.value.isNull();
    }

    @Override
    public boolean isMissing() {
        return this.value.isMissingNode();
    }

    @Override
    public Boolean asBoolean() {
        return this.value.isBoolean() ? this.value.booleanValue() : null;
    }

    @Override
    public Integer asInt() {
        return this.value.isNumber() ? this.value.intValue() : null;
    }

    @Override
    public Long asLong() {
        return this.value.isNumber() ? this.value.longValue() : null;
    }

    @Override
    public Double asDouble() {
        return this.value.isNumber() ? this.value.doubleValue() : null;
    }

    @Override
    public String asString() {
        return this.value.textValue();
    }

    @Override
    public Date asDate() {
        Instant instant = asInstant();
        try {
            return instant == null ? null : Date.from(instant);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Instant asInstant() {
        if (!this.value.canConvertToLong()) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(this.value.longValue());
        } catch (DateTimeException e) {
            return null;
        }
    }

    @Override
    public <T> T[] asArray(Class<T> type) {
        return this.value.isArray() ? readAs(MAPPER.getTypeFactory().constructArrayType(type)) : null;
    }

    @Override
    public <T> List<T> asList(Class<T> type) {
        return this.value.isArray() ? readAs(MAPPER.getTypeFactory().constructCollectionType(List.class, type)) : null;
    }

    @Override
    public Map<String, Object> asMap() {
        return this.value.isObject()
                ? readAs(MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class))
                : null;
    }

    @Override
    public <T> T as(Class<T> type) {
        if (isNull() || isMissing()) {
            return null;
        }
        if (JsonNode.class.isAssignableFrom(type) && type.isInstance(this.value)) {
            return type.cast(this.value.deepCopy());
        }
        return readAs(MAPPER.constructType(type));
    }

    @Override
    public @NonNull String toString() {
        return this.value.toString();
    }

    private <T> T readAs(JavaType type) {
        try {
            return MAPPER.readerFor(type).readValue(this.value);
        } catch (IOException e) {
            throw new JWTDecodeException("Cannot read JWT claim as " + type, e);
        }
    }
}
