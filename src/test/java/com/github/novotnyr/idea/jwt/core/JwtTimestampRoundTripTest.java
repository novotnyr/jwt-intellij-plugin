package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JwtTimestampRoundTripTest {
    private final JwtDecoder decoder = new JwtDecoder();

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

    private static Jwt token(String payload) {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        return new Jwt(header + "." + encode(payload) + ".c2ln");
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
