package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class Hacking {
    public static boolean isInstanceOfJWTDecoder(DecodedJWT decodedJwt) {
        return decodedJwt.getClass().getName().equals("com.auth0.jwt.JWTDecoder");
    }

    public static Map<String, Claim> getClaims(DecodedJWT decodedJwt) {
        try {
            //noinspection JavaReflectionMemberAccess
            Field payloadField = decodedJwt.getClass().getDeclaredField("payload");
            payloadField.setAccessible(true);
            Object payload = payloadField.get(decodedJwt);
            Field treeField = payload.getClass().getDeclaredField("tree");
            treeField.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String, JsonNode> tree = (Map<String, JsonNode>) treeField.get(payload);
            Map<String, Claim> claims = new LinkedHashMap<>();
            for (String name : tree.keySet()) {
                claims.put(name, _claimFromNode(tree.get(name)));
            }
            return claims;
        } catch (Exception e) {
            throw new IllegalArgumentException("Reflection failed on JWT", e);
        }
    }

    public static JsonNode asJsonNode(Claim claim) {
        try {
            Field dataField = claim.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            return (JsonNode) dataField.get(claim);
        } catch (Exception e) {
            throw new IllegalArgumentException("Reflection failed on JWT Claim " + claim.toString(), e);
        }
    }

    private static Claim _claimFromNode(JsonNode node) {
        if (NullClaim.supports(node)) {
            return NullClaim.of(node);
        } else {
            return _newJsonNodeClaim(node);
        }
    }

    private static Claim _newJsonNodeClaim(JsonNode node) {
        try {
            //noinspection JavaReflectionMemberAccess
            Constructor<?> constructor = Class.forName("com.auth0.jwt.impl.JsonNodeClaim")
                                              .getConstructor(JsonNode.class);
            return (Claim) constructor.newInstance(node);
        } catch (Exception e) {
            return NullClaim.EMPTY_AND_MISSING;
        }
    }
}
