package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class NullClaim implements Claim {
    public static final NullClaim EMPTY_AND_MISSING = new NullClaim(true, true);

    private boolean isNull;

    private boolean isMissing;

    public NullClaim(boolean isNull, boolean isMissing) {
        this.isNull = isNull;
        this.isMissing = isMissing;
    }

    public static NullClaim of(@Nullable JsonNode node) {
        if (node == null || node.isNull()) {
            return new NullClaim(true, false);
        }
        return new NullClaim(node.isNull(), node.isMissingNode());
    }

    public static boolean supports(@Nullable JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isMissing() {
        return false;
    }

    @Override
    public Boolean asBoolean() {
        return null;
    }

    @Override
    public Integer asInt() {
        return 0;
    }

    @Override
    public Long asLong() {
        return 0L;
    }

    @Override
    public Double asDouble() {
        return 0.0;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public Date asDate() {
        return null;
    }

    @Override
    public <T> T[] asArray(Class<T> clazz) throws JWTDecodeException {
        return null;
    }

    @Override
    public <T> List<T> asList(Class<T> clazz) throws JWTDecodeException {
        return List.of();
    }

    @Override
    public Map<String, Object> asMap() throws JWTDecodeException {
        return Map.of();
    }

    @Override
    public <T> T as(Class<T> clazz) throws JWTDecodeException {
        return null;
    }
}
