package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.impl.NullClaim;
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
            Map<String, JsonNode> tree = (Map<String, JsonNode>) treeField.get(payload);
            Map<String, Claim> claims = new LinkedHashMap<>();
            for (String name : tree.keySet()) {
                claims.put(name, _claimFromNode(tree.get(name)));
            }
            return claims;
        } catch (Exception e) {
            throw new IllegalArgumentException("Reflection failed on JWT", e);
        }
    }

    private static Claim _claimFromNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return new NullClaim();
        }
        return _newJsonNodeClaim(node);
    }

    private static Claim _newJsonNodeClaim(JsonNode node) {
        try {
            //noinspection JavaReflectionMemberAccess
            Constructor<?> constructor = Class.forName("com.auth0.jwt.impl.JsonNodeClaim")
                    .getDeclaredConstructor(JsonNode.class);
            constructor.setAccessible(true);
            return (Claim) constructor.newInstance(node);
        } catch (Exception e) {
            return new NullClaim();
        }
    }

}
