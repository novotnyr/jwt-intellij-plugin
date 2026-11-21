package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JwtHeaderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void readsEveryHeaderParameterInJsonOrder() {
        String header = "{\"kid\":\"key-1\",\"alg\":\"HS256\",\"custom\":{\"enabled\":true},"
                + "\"audiences\":[\"one\",\"two\"],\"number\":1.2345678901234567890123456789,"
                + "\"flag\":true,\"nullable\":null}";
        Jwt jwt = new Jwt(signedToken(header));

        assertEquals(List.of("kid", "alg", "custom", "audiences", "number", "flag", "nullable"),
                jwt.getHeaderClaims().stream().map(NamedClaim::getName).toList());
        assertEquals("key-1", jwt.getHeaderClaims().get(0).getValueString());
        assertEquals("{\"enabled\":true}", jwt.getHeaderClaims().get(2).getValueString());
        assertEquals("[\"one\",\"two\"]", jwt.getHeaderClaims().get(3).getValueString());
        assertEquals("1.2345678901234567890123456789", jwt.getHeaderClaims().get(4).getValueString());
        assertEquals("true", jwt.getHeaderClaims().get(5).getValueString());
        assertEquals("null", jwt.getHeaderClaims().get(6).getValueString());
    }

    @Test
    public void preservesHeaderParametersWhenPayloadChanges() throws IOException {
        String header = "{\"kid\":\"key-1\",\"alg\":\"HS256\",\"custom\":{\"enabled\":true},"
                + "\"audiences\":[\"one\",\"two\"],\"number\":1.2345678901234567890123456789}";
        Jwt jwt = new Jwt(signedToken(header));
        JsonNode originalHeader = decodedHeader(jwt);

        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setPayloadClaim(new StringClaim("subject", "alice"));

        assertEquals(originalHeader, decodedHeader(jwt));
        assertEquals("alice", JWT.require(Algorithm.HMAC256("secret")).build().verify(jwt.toString())
                .getClaim("subject").asString());
    }

    @Test
    public void editableHeaderValueDoesNotChangeTheDecodedSnapshot() {
        Jwt jwt = new Jwt(signedToken("{\"alg\":\"HS256\",\"custom\":{\"enabled\":true}}"));
        ObjectNode editableValue = (ObjectNode) jwt.getHeaderClaims().get(1).getValue();
        editableValue.put("enabled", false);

        JsonNode decodedValue = jwt.getDecodedJwt().getHeaderClaim("custom").as(JsonNode.class);
        assertTrue(decodedValue.get("enabled").booleanValue());
    }

    private static String signedToken(String headerJson) {
        String header = encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = encode("{}".getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = encode(Algorithm.HMAC256("secret").sign(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }

    private static JsonNode decodedHeader(Jwt jwt) throws IOException {
        return MAPPER.readTree(Base64.getUrlDecoder().decode(JWT.decode(jwt.toString()).getHeader()));
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
