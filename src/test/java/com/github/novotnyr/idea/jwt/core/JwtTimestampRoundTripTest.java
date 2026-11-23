package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.novotnyr.idea.jwt.SecretNotSpecifiedException;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JwtTimestampRoundTripTest {
    private static final String MIXED_PAYLOAD = """
            {
              "exp": 1735689601.125,
              "iat": "123",
              "nbf": false,
              "sub": "alice",
              "active": true,
              "count": 42,
              "fraction": 42.75,
              "large": 9223372036854775808,
              "unset": null,
              "options": {"enabled": true},
              "roles": ["reader"]
            }
            """;

    private final JwtDecoder decoder = new JwtDecoder();

    @Test
    public void mixedPayloadKeepsEditorTypesAndIsolatesMutableValues() {
        Jwt jwt = token(MIXED_PAYLOAD);

        List<NamedClaim<?>> claims = jwt.getPayloadClaims();
        JsonNodeFactory nodes = JsonNodeFactory.instance;
        assertEquals(List.of(
                DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.125")),
                new StringClaim("iat", "123"),
                new RawClaim("nbf", BooleanNode.FALSE),
                new StringClaim("sub", "alice"),
                new BooleanClaim("active", true),
                new NumericClaim("count", 42L),
                new NumericClaim("fraction", new BigDecimal("42.75")),
                new NumericClaim("large", new BigDecimal("9223372036854775808")),
                new StringClaim("unset", null),
                new RawClaim("options", nodes.objectNode().put("enabled", true)),
                new RawClaim("roles", nodes.arrayNode().add("reader"))
        ), claims);

        ObjectNode editableOptions = (ObjectNode) claims.get(9).getValue();
        ArrayNode editableRoles = (ArrayNode) claims.getLast().getValue();
        editableOptions.put("enabled", false);
        editableRoles.add("writer");

        JsonNode options = jwt.getDecodedJwt().getClaim("options").as(JsonNode.class);
        assertTrue(options.get("enabled").booleanValue());
        assertEquals(List.of("reader"), jwt.getDecodedJwt().getClaim("roles").asList(String.class));
    }

    @Test
    public void signingAnEditPreservesUntouchedJsonValuesAndRefreshesTheRows() {
        Jwt jwt = token(MIXED_PAYLOAD);
        ObjectNode expected = this.decoder.parsePayload(jwt.getPayloadString());
        expected.put("sub", "bob");

        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setPayloadClaim(new StringClaim("sub", "bob"));

        assertEquals(expected, this.decoder.parsePayload(jwt.getPayloadString()));
        assertEquals(new Jwt(jwt.toString()).getPayloadClaims(), jwt.getPayloadClaims());
    }

    @Test
    public void removingIssuedAtPreservesAudienceArrayAndProducesValidSignature() {
        Jwt jwt = token("{\"iat\":1735689600,\"aud\":[\"one\",\"two\"],\"sub\":\"alice\"}");
        ObjectNode expected = this.decoder.parsePayload(jwt.getPayloadString());
        expected.remove("iat");
        jwt.setSignatureContext(new HS256SignatureContext("secret"));

        jwt.removeClaim("iat");

        assertEquals(expected, this.decoder.parsePayload(jwt.getPayloadString()));
        Algorithm.HMAC256("secret").verify(jwt.getDecodedJwt());
    }

    @Test
    public void removingIssuedAtPreservesJsonAudienceShapes() {
        for (String audience : List.of("\"service\"", "[\"service\"]", "[]", "[\"service\",{\"id\":1}]")) {
            Jwt jwt = token("{\"iat\":1735689600,\"aud\":" + audience + ",\"sub\":\"alice\"}");
            ObjectNode expected = this.decoder.parsePayload(jwt.getPayloadString());
            expected.remove("iat");
            jwt.setSignatureContext(new HS256SignatureContext("secret"));

            jwt.removeClaim("iat");

            assertEquals("audience " + audience, expected, this.decoder.parsePayload(jwt.getPayloadString()));
        }
    }

    @Test
    public void builderKeepsLegacyJavaAudienceArrayNormalization() {
        assertEquals("service", builderPayload(new String[]{"service"}).get("aud").textValue());
        assertFalse(builderPayload(new String[0]).has("aud"));
        assertEquals("[\"one\",\"two\"]", builderPayload(new String[]{"one", "two"}).get("aud").toString());
    }

    @Test
    public void failedSigningKeepsTheOriginalSnapshotAndRows() {
        Jwt jwt = token("{\"sub\":\"alice\"}");
        DecodedJWT snapshot = jwt.getDecodedJwt();
        List<NamedClaim<?>> rows = List.copyOf(jwt.getPayloadClaims());

        assertThrows(SecretNotSpecifiedException.class, () -> jwt.setPayloadClaim(new StringClaim("sub", "bob")));
        assertEquals(rows, jwt.getPayloadClaims());

        assertThrows(SecretNotSpecifiedException.class, () -> jwt.removeClaim("sub"));
        assertEquals(rows, jwt.getPayloadClaims());
        assertSame(snapshot, jwt.getDecodedJwt());
    }

    @Test
    public void editingAnotherClaimPreservesFractionalNumericDatesAsJsonNumbers() {
        Jwt jwt = token("{\"exp\":1735689601.123456789,\"nbf\":1735689500.25,\"iat\":1735689400.75}");
        assertTrue(jwt.getPayloadClaims().stream().allMatch(DateClaim.class::isInstance));
        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setPayloadClaim(new StringClaim("subject", "alice"));

        JsonNode payload = this.decoder.parsePayload(jwt.getPayloadString());
        assertNumericDate(payload.get("exp"), "1735689601.123456789");
        assertNumericDate(payload.get("nbf"), "1735689500.25");
        assertNumericDate(payload.get("iat"), "1735689400.75");
    }

    @Test
    public void editingAnotherClaimKeepsInvalidNullTimestampAsNull() {
        Jwt jwt = token("{\"exp\":null}");
        assertTrue(jwt.getPayloadClaims().getFirst() instanceof RawClaim);
        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setPayloadClaim(new StringClaim("subject", "alice"));

        Claim expiration = JWT.decode(jwt.toString()).getClaim("exp");
        assertFalse(expiration.isMissing());
        assertTrue(expiration.isNull());
    }

    @Test
    public void copyingAndEditingTimestampRetainsFractionalSeconds() {
        DateClaim original = DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.125"));
        assertEquals(0, original.getValue().compareTo(original.copy().getValue()));
        DateClaim edited = (DateClaim) com.github.novotnyr.idea.jwt.ClaimUtils.copyClaim(
                original, "exp", new BigDecimal("1735689602.375"));
        assertEquals(0, new BigDecimal("1735689602.375").compareTo(edited.getValue()));
    }

    @Test
    public void customTimestampClaimRemainsNumeric() {
        Jwt jwt = new Jwt();
        jwt.setAlgorithm("HS256");
        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setPayloadClaim(new DateClaim("customTimestamp", new Date(1735689601000L)));

        JsonNode payload = this.decoder.parsePayload(jwt.getPayloadString());
        assertNumericDate(payload.get("customTimestamp"), "1735689601");
        assertEquals(new NumericClaim("customTimestamp", 1735689601L), jwt.getPayloadClaims().getFirst());
    }

    @Test
    public void stringIssuedAtCanBeAddedAndSurvivesReloadAndAnotherEdit() {
        HS256SignatureContext context = new HS256SignatureContext("secret");
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256, context, true);
        jwt.removeClaim("iat");
        jwt.setPayloadClaim(new StringClaim("iat", "yes"));
        assertEquals("yes", this.decoder.parsePayload(jwt.getPayloadString()).get("iat").textValue());

        Jwt reloaded = new Jwt(jwt.toString());
        assertEquals(new StringClaim("iat", "yes"), reloaded.getPayloadClaims().getFirst());
        reloaded.setSignatureContext(context);
        reloaded.setPayloadClaim(new StringClaim("sub", "alice"));
        assertEquals("yes", this.decoder.parsePayload(reloaded.getPayloadString()).get("iat").textValue());
    }

    private static void assertNumericDate(JsonNode claim, String expected) {
        assertTrue(claim.isNumber());
        assertEquals(0, new BigDecimal(expected).compareTo(claim.decimalValue()));
    }

    private ObjectNode builderPayload(String[] audience) {
        String signed = new JwtBuilder()
                .withClaim(javaArrayAudience(audience))
                .sign("HS256", new HS256SignatureContext("secret"));
        return this.decoder.parsePayload(signed.split("\\.")[1]);
    }

    private static NamedClaim<String[]> javaArrayAudience(String[] audience) {
        return new NamedClaim<>("aud", audience) {
            @Override
            public NamedClaim<String[]> copy() {
                return javaArrayAudience(getValue());
            }
        };
    }

    private static Jwt token(String payload) {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        return new Jwt(header + "." + encode(payload) + ".c2ln");
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
