package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtDecoder;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JwtValidatorTest {
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00.500Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final BigDecimal NOW_SECONDS = NumericDate.fromInstant(NOW);

    private static final String SIGNING_SECRET = "test secret";

    @Test
    public void missingTimestampClaimsAreAllowed() {
        assertTrue(validate("{}").claimErrors().isEmpty());
    }

    @Test
    public void expirationRequiresNowToBeStrictlyBeforeIt() {
        assertErrors("{\"exp\":" + NOW_SECONDS + "}", "exp");
        assertTrue(validate("{\"exp\":" + NOW_SECONDS.add(new BigDecimal("0.001")) + "}")
                .claimErrors().isEmpty());
        assertTrue(validate("{\"exp\":" + NOW_SECONDS.add(new BigDecimal("0.000000001")) + "}")
                .claimErrors().isEmpty());
    }

    @Test
    public void notBeforeAllowsEqualityButRejectsFutureTime() {
        assertTrue(validate("{\"nbf\":" + NOW_SECONDS + "}").claimErrors().isEmpty());
        assertErrors("{\"nbf\":" + NOW_SECONDS.add(new BigDecimal("0.001")) + "}", "nbf");
    }

    @Test
    public void futureIssuedAtIsAllowedWhenItIsNumeric() {
        String payload = "{\"exp\":" + NOW_SECONDS.add(new BigDecimal("0.125"))
                + ",\"nbf\":" + NOW_SECONDS.subtract(new BigDecimal("0.125"))
                + ",\"iat\":" + NOW_SECONDS.add(new BigDecimal("3600.25")) + "}";
        assertTrue(validate(payload).claimErrors().isEmpty());
        assertTrue(validate("{\"iat\":-0.5}").claimErrors().isEmpty());
    }

    @Test
    public void issuedAtIsOptionalAndMustBeANumericDate() {
        JwtClaimsValidator validator = new JwtClaimsValidator(CLOCK);

        assertTrue(validator.validate(JWT.decode(encodedToken("{\"nbf\":0}"))).isEmpty());
        assertTrue(validator.validate(JWT.decode(encodedToken("{\"iat\":0}"))).isEmpty());
        assertTrue(validator.validate(JWT.decode(encodedToken("{\"iat\":0.25}"))).isEmpty());

        List<ClaimError> nullIssuedAtErrors = validator.validate(JWT.decode(encodedToken("{\"iat\":null}")));
        assertEquals(List.of("iat"), nullIssuedAtErrors.stream().map(ClaimError::getClaim).toList());

        DecodedJWT stringIssuedAt = new JwtDecoder(CLOCK).decode(encodedToken("{\"iat\":\"0\"}")).decodedJwt();
        List<ClaimError> stringIssuedAtErrors = validator.validate(stringIssuedAt);
        assertEquals(List.of("iat"), stringIssuedAtErrors.stream().map(ClaimError::getClaim).toList());
    }

    @Test
    public void parsesAndValidatesStringIssuedAtFixture() throws IOException {
        String encoded;
        try (InputStream fixture = JwtValidatorTest.class.getResourceAsStream("/string-iat.jwt")) {
            assertNotNull("Missing test resource string-iat.jwt", fixture);
            encoded = new String(fixture.readAllBytes(), StandardCharsets.UTF_8).strip();
        }

        Jwt jwt = new Jwt(encoded);
        assertEquals(new StringClaim("iat", "1516239022"), jwt.getPayloadClaims().stream()
                .filter(claim -> "iat".equals(claim.getName()))
                .findFirst()
                .orElseThrow());

        List<ClaimError> errors = new JwtValidator(CLOCK).validateClaims(jwt).claimErrors();
        assertEquals(List.of("iat"), errors.stream().map(ClaimError::getClaim).toList());
        assertEquals("Illegal type: NumericDate must be a JSON number", errors.getFirst().getMessage());
    }

    @Test
    public void explicitNullIsInvalidButAbsenceIsAllowed() {
        assertErrors("{\"exp\":null,\"nbf\":null,\"iat\":null}", "exp", "nbf", "iat");
    }

    @Test
    public void nonNumericTimestampClaimsAreReported() {
        assertErrors("{\"exp\":\"123\"}", "exp");
        assertErrors("{\"nbf\":true}", "nbf");
        assertErrors("{\"iat\":[]}", "iat");
        assertErrors("{\"exp\":\"bad\",\"nbf\":false,\"iat\":{}}", "exp", "nbf", "iat");
    }

    @Test
    public void invalidIssuedAtStillVerifiesTheOriginalSignedPayload() {
        HS256SignatureContext context = new HS256SignatureContext("secret");
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256, context, true);
        jwt.removeClaim("iat");
        DecodedJWT beforeEdit = jwt.getDecodedJwt();
        jwt.setPayloadClaim(new StringClaim("iat", "yes"));

        assertNotSame(beforeEdit, jwt.getDecodedJwt());
        assertEquals("yes", jwt.getDecodedJwt().getClaim("iat").asString());

        JwtValidator validator = new JwtValidator(CLOCK);
        JwtErrors validSignature = validator.validate(jwt, context);
        assertEquals(List.of("iat"), validSignature.claimErrors().stream().map(ClaimError::getClaim).toList());
        assertEquals("Illegal type: NumericDate must be a JSON number",
                validSignature.claimErrors().getFirst().getMessage());
        assertFalse(validSignature.hasSignatureError());

        String[] parts = jwt.toString().split("\\.");
        Jwt tampered = new Jwt(parts[0] + "." + encode("{\"iat\":\"no\"}") + "." + parts[2]);
        JwtErrors invalidSignature = validator.validate(tampered, context);
        assertTrue(invalidSignature.hasSignatureError());
    }

    @Test
    public void validatingTheSameSnapshotAtALaterTimeReportsExpiration() {
        Jwt jwt = token("{\"exp\":" + NumericDate.fromInstant(NOW.plusSeconds(1)) + "}");
        DecodedJWT snapshot = jwt.getDecodedJwt();

        assertTrue(new JwtValidator(CLOCK).validateClaims(jwt).claimErrors().isEmpty());

        JwtValidator laterValidator = new JwtValidator(Clock.offset(CLOCK, Duration.ofSeconds(1)));
        List<ClaimError> errors = laterValidator.validateClaims(jwt).claimErrors();
        assertEquals(List.of("exp"), errors.stream().map(ClaimError::getClaim).toList());
        assertSame(snapshot, jwt.getDecodedJwt());
    }

    @Test
    public void decodedJwtClaimErrorsAreIndependentPerCall() {
        JwtClaimsValidator validator = new JwtClaimsValidator(CLOCK);
        List<ClaimError> expired = validator.validate(JWT.decode(encodedToken("{\"exp\":0}")));
        assertTrue(validator.validate(JWT.decode(encodedToken("{}"))).isEmpty());
        assertEquals(List.of("exp"), expired.stream().map(ClaimError::getClaim).toList());
    }

    @Test
    public void validatesDecodedClaimsWithoutReadingEncodedPayload() {
        String payload = "{\"exp\":" + NumericDate.fromInstant(NOW.plusNanos(1)) + ",\"iat\":\"123\"}";
        DecodedJWT decoded = new JwtDecoder(CLOCK).decode(encodedToken(payload)).decodedJwt();
        DecodedJWT jwt = (DecodedJWT) Proxy.newProxyInstance(
                DecodedJWT.class.getClassLoader(), new Class<?>[]{DecodedJWT.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getPayload")) {
                        throw new AssertionError("The encoded payload should not be read during claim validation");
                    }
                    return method.invoke(decoded, args);
                });

        List<ClaimError> errors = new JwtClaimsValidator(CLOCK).validate(jwt);
        assertEquals(List.of("iat"), errors.stream().map(ClaimError::getClaim).toList());
    }

    @Test
    public void signatureVerificationStillRunsWithOptionalClaimsMissing() {
        Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("right secret"), false);
        JwtValidator validator = new JwtValidator(CLOCK);
        JwtErrors errors = validator.validate(new Jwt(signed.toString()), new HS256SignatureContext("wrong secret"));
        assertTrue(errors.claimErrors().isEmpty());
        assertTrue(errors.hasSignatureError());
        assertEquals("Signature failed", errors.signatureError().message());
    }

    @Test
    public void decodedJwtWithValidSignatureAndClaimsHasNoErrors() {
        DecodedJWT jwt = signedToken(NOW.plusSeconds(60));
        JwtValidator validator = new JwtValidator(CLOCK);

        JwtErrors errors = validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET));

        assertTrue(errors.claimErrors().isEmpty());
        assertFalse(errors.hasSignatureError());
        assertNull(errors.signatureError());
    }

    @Test
    public void decodedJwtReportsExpirationErrorWithValidSignature() {
        DecodedJWT jwt = signedToken(Instant.EPOCH);
        JwtValidator validator = new JwtValidator(CLOCK);

        JwtErrors errors = validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET));

        assertEquals(List.of("exp"), errors.claimErrors().stream().map(ClaimError::getClaim).toList());
        assertFalse(errors.hasSignatureError());
    }

    @Test
    public void decodedJwtExpirationUsesThePrecisionRetainedByItsDecoder() {
        HS256SignatureContext context = new HS256SignatureContext(SIGNING_SECRET);
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256, context, false);
        jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", NumericDate.fromInstant(NOW.plusNanos(1))));
        JwtValidator validator = new JwtValidator(CLOCK);

        JwtErrors pluginErrors = validator.validate(jwt.getDecodedJwt(), context);
        JwtErrors auth0Errors = validator.validate(JWT.decode(jwt.toString()), context);

        assertTrue(pluginErrors.claimErrors().isEmpty());
        assertFalse(pluginErrors.hasSignatureError());

        // Auth0 4.5.1 rounds this expiration to the clock time.
        assertEquals(List.of("exp"), auth0Errors.claimErrors().stream().map(ClaimError::getClaim).toList());
        assertEquals("Expiration time has passed", auth0Errors.claimErrors().getFirst().getMessage());
        assertFalse(auth0Errors.hasSignatureError());
    }

    @Test
    public void decodedJwtReportsBothExpirationAndSignatureErrors() {
        DecodedJWT jwt = signedToken(Instant.EPOCH);
        JwtValidator validator = new JwtValidator(CLOCK);

        JwtErrors errors = validator.validate(jwt, new HS256SignatureContext("wrong secret"));

        assertEquals(List.of("exp"), errors.claimErrors().stream().map(ClaimError::getClaim).toList());
        assertTrue(errors.hasSignatureError());
        assertEquals("Signature failed", errors.signatureError().message());
    }

    @Test
    public void reusedValidatorReturnsIndependentResults() {
        JwtValidator validator = new JwtValidator(CLOCK);

        JwtErrors invalid = validator.validate(signedToken(Instant.EPOCH), new HS256SignatureContext("wrong secret"));
        JwtErrors claimsOnly = validator.validateClaims(token("{}"));
        JwtErrors valid = validator.validate(signedToken(NOW.plusSeconds(60)),
                new HS256SignatureContext(SIGNING_SECRET));

        assertEquals(List.of("exp"), invalid.claimErrors().stream().map(ClaimError::getClaim).toList());
        assertEquals("Signature failed", invalid.signatureError().message());
        assertTrue(claimsOnly.claimErrors().isEmpty());
        assertFalse(claimsOnly.hasSignatureError());
        assertTrue(valid.claimErrors().isEmpty());
        assertFalse(valid.hasSignatureError());
    }

    @Test
    public void signatureValidatorAcceptsValidSignatureOnExpiredJwt() {
        DecodedJWT jwt = signedToken(Instant.EPOCH);
        JwtSignatureValidator validator = new JwtSignatureValidator();
        assertTrue(validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET)).isEmpty());
    }

    private static JwtErrors validate(String payload) {
        return new JwtValidator(CLOCK).validateClaims(token(payload));
    }

    private static void assertErrors(String payload, String... claimNames) {
        List<String> actual = validate(payload).claimErrors().stream().map(ClaimError::getClaim).toList();
        assertEquals(List.of(claimNames), actual);
    }

    private static Jwt token(String payload) {
        return new Jwt(encodedToken(payload));
    }

    private static DecodedJWT signedToken(Instant expiresAt) {
        return JWT.decode(JWT.create()
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(SIGNING_SECRET)));
    }

    private static String encodedToken(String payload) {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        return header + "." + encode(payload) + ".c2ln";
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
