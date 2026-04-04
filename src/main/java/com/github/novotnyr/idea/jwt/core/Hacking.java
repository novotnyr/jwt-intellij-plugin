package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Field;

public class Hacking {
    public static JsonNode asJsonNode(Claim claim) {
        try {
            Field dataField = claim.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            return (JsonNode) dataField.get(claim);
        } catch (Exception e) {
            throw new IllegalArgumentException("Reflection failed on JWT Claim " + claim.toString(), e);
        }
    }
}
