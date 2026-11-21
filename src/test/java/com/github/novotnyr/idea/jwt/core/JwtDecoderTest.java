package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.Charset;
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
    private static final String HEADER = "{\"alg\":\"HS256\"}";

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

    @Test
    public void decodesUnicodeInHeaderAndPayload() {
        String header = encode("{\"alg\":\"HS256\",\"kid\":\"kľúč\"}");
        String payload = encode("{\"sub\":\"Žofia 🌍\"}");
        DecodedJWT jwt = this.decoder.decode(header + "." + payload + ".c2ln").decodedJwt();

        assertEquals("kľúč", jwt.getKeyId());
        assertEquals("Žofia 🌍", jwt.getSubject());
    }

    @Test
    public void rejectsUtf16AndUtf32Segments() {
        String header = encode(HEADER);
        String payload = encode("{}");
        for (Charset charset : List.of(StandardCharsets.UTF_16LE, Charset.forName("UTF-32BE"))) {
            String nonUtf8Header = encode(HEADER.getBytes(charset));
            String nonUtf8Payload = encode("{}".getBytes(charset));
            assertThrows("Header encoded as " + charset, JWTDecodeException.class,
                    () -> this.decoder.decode(nonUtf8Header + "." + payload + ".c2ln"));
            assertThrows("Payload encoded as " + charset, JWTDecodeException.class,
                    () -> this.decoder.decode(header + "." + nonUtf8Payload + ".c2ln"));
        }
    }

    @Test
    public void rejectsMalformedUtf8InsideAStringClaim() {
        byte[] payload = "{\"sub\":\"?\"}".getBytes(StandardCharsets.UTF_8);
        payload[payload.length - 3] = (byte) 0x80; // Continuation byte without a leading byte.
        String encoded = encode(HEADER) + "." + encode(payload) + ".c2ln";

        JWTDecodeException error = assertThrows(JWTDecodeException.class, () -> this.decoder.decode(encoded));
        assertEquals("The JWT payload must be UTF-8 encoded", error.getMessage());
    }

    private static String token(String payload) {
        return encode(HEADER) + "." + encode(payload) + ".c2ln";
    }

    private static String encode(String json) {
        return encode(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
