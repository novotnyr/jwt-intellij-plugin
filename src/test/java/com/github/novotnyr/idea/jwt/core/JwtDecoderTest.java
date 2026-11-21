package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JwtDecoderTest {
    private final JwtDecoder decoder = new JwtDecoder();

    @Test
    public void invalidTimestampsKeepTheirOriginalValuesAlongsideClaimErrors() {
        String encoded = token("{\"iat\":\"yes\",\"exp\":{\"value\":1},\"nbf\":false}");
        JsonWebToken result = this.decoder.decode(encoded);
        DecodedJWT jwt = result.decodedJwt();

        assertEquals(encoded, jwt.getToken());
        assertEquals(encoded.split("\\.")[1], jwt.getPayload());
        assertFalse(jwt.getClaim("iat").isNull());
        assertEquals("yes", jwt.getClaim("iat").asString());
        assertEquals("yes", jwt.getClaims().get("iat").asString());
        assertNull(jwt.getIssuedAt());
        assertEquals(Map.of("value", BigInteger.ONE), jwt.getClaim("exp").asMap());
        assertEquals(Boolean.FALSE, jwt.getClaim("nbf").asBoolean());
        assertEquals(List.of("exp", "nbf", "iat"),
                result.errors().claimErrors().stream().map(ClaimError::getClaim).toList());
        assertFalse(result.errors().hasSignatureError());
    }

    @Test
    public void explicitNullIsReportedAndMissingTimestampIsAllowed() {
        JsonWebToken result = this.decoder.decode(token("{\"iat\":null}"));

        assertTrue(result.decodedJwt().getClaim("iat").isNull());
        assertFalse(result.decodedJwt().getClaim("iat").isMissing());
        assertTrue(result.decodedJwt().getClaim("nbf").isMissing());
        assertEquals(List.of("iat"), result.errors().claimErrors().stream().map(ClaimError::getClaim).toList());
    }

    @Test
    public void validTokenKeepsTypedHeaderAndPayloadClaims() {
        String encoded = JWT.create()
                .withKeyId("key")
                .withIssuer("issuer")
                .withAudience("one", "two")
                .withIssuedAt(Instant.EPOCH)
                .sign(Algorithm.HMAC256("secret"));
        JsonWebToken result = this.decoder.decode(encoded);
        DecodedJWT jwt = result.decodedJwt();

        assertEquals("HS256", jwt.getAlgorithm());
        assertEquals("key", jwt.getKeyId());
        assertEquals("key", jwt.getHeaderClaim("kid").asString());
        assertEquals("issuer", jwt.getIssuer());
        assertEquals(List.of("one", "two"), jwt.getAudience());
        assertEquals(Instant.EPOCH, jwt.getIssuedAtAsInstant());
        assertTrue(result.errors().claimErrors().isEmpty());
    }

    @Test
    public void invalidPayloadSyntaxStillFailsDecoding() {
        assertThrows(JWTDecodeException.class, () -> this.decoder.decode(token("[]")));
        assertThrows(JWTDecodeException.class, () -> this.decoder.decode(token("{")));
    }

    private static String token(String payload) {
        return encode("{\"alg\":\"HS256\"}") + "." + encode(payload) + ".c2ln";
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
