package com.github.novotnyr.idea.jwt.validation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JwtValidatorTest {
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00.500Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final BigDecimal NOW_SECONDS = NumericDate.fromInstant(NOW);

    private static final String SIGNING_SECRET = "test secret";

    @Test
    public void missingTimestampClaimsAreAllowed() {
        assertTrue(validate("{}").getClaimErrors().isEmpty());
    }

    @Test
    public void expirationRequiresNowToBeStrictlyBeforeIt() {
        assertErrors("{\"exp\":" + NOW_SECONDS + "}", "exp");
        assertTrue(validate("{\"exp\":" + NOW_SECONDS.add(new BigDecimal("0.001")) + "}")
                .getClaimErrors().isEmpty());
        assertTrue(validate("{\"exp\":" + NOW_SECONDS.add(new BigDecimal("0.000000001")) + "}")
                .getClaimErrors().isEmpty());
    }

    @Test
    public void notBeforeAllowsEqualityButRejectsFutureTime() {
        assertTrue(validate("{\"nbf\":" + NOW_SECONDS + "}").getClaimErrors().isEmpty());
        assertErrors("{\"nbf\":" + NOW_SECONDS.add(new BigDecimal("0.001")) + "}", "nbf");
    }

    @Test
    public void futureIssuedAtIsAllowedWhenItIsNumeric() {
        String payload = "{\"exp\":" + NOW_SECONDS.add(new BigDecimal("0.125"))
                + ",\"nbf\":" + NOW_SECONDS.subtract(new BigDecimal("0.125"))
                + ",\"iat\":" + NOW_SECONDS.add(new BigDecimal("3600.25")) + "}";
        assertTrue(validate(payload).getClaimErrors().isEmpty());
        assertTrue(validate("{\"iat\":-0.5}").getClaimErrors().isEmpty());
    }

    @Test
    public void issuedAtIsOptionalAndMustBeANumericDate() {
        JwtClaimsValidator validator = new JwtClaimsValidator(CLOCK);

        assertTrue(validator.validate(JWT.decode(encodedToken("{\"nbf\":0}"))).isEmpty());
        assertTrue(validator.validate(JWT.decode(encodedToken("{\"iat\":0}"))).isEmpty());
        assertTrue(validator.validate(JWT.decode(encodedToken("{\"iat\":0.25}"))).isEmpty());

        List<ClaimError> nullIssuedAtErrors = validator.validate(JWT.decode(encodedToken("{\"iat\":null}")));
        assertEquals(List.of("iat"), nullIssuedAtErrors.stream().map(ClaimError::getClaim).toList());
        assertThrows(JWTDecodeException.class, () -> JWT.decode(encodedToken("{\"iat\":\"0\"}")));
    }

    @Test
    public void explicitNullIsInvalidButAbsenceIsAllowed() {
        assertErrors("{\"exp\":null,\"nbf\":null,\"iat\":null}", "exp", "nbf", "iat");
    }

    @Test
    public void nonNumericTimestampClaimsAreRejected() {
        assertThrows(JWTDecodeException.class, () -> token("{\"exp\":\"123\"}"));
        assertThrows(JWTDecodeException.class, () -> token("{\"nbf\":true}"));
        assertThrows(JWTDecodeException.class, () -> token("{\"iat\":[]}"));
    }

    @Test
    public void decodedJwtClaimErrorsAreIndependentPerCall() {
        JwtClaimsValidator validator = new JwtClaimsValidator(CLOCK);
        List<ClaimError> expired = validator.validate(JWT.decode(encodedToken("{\"exp\":0}")));
        assertTrue(validator.validate(JWT.decode(encodedToken("{}"))).isEmpty());
        assertEquals(List.of("exp"), expired.stream().map(ClaimError::getClaim).toList());
    }

    @Test
    public void signatureVerificationStillRunsWithOptionalClaimsMissing() {
        Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("right secret"), false);
        JwtValidator validator = new JwtValidator(CLOCK);
        validator.validate(new Jwt(signed.toString()), new HS256SignatureContext("wrong secret"));
        assertTrue(validator.getClaimErrors().isEmpty());
        assertTrue(validator.hasSignatureError());
        assertEquals("Signature failed", validator.getSignatureError().getMessage());
    }

    @Test
    public void decodedJwtWithValidSignatureAndClaimsHasNoErrors() {
        DecodedJWT jwt = signedToken(NOW.plusSeconds(60));
        JwtValidator validator = new JwtValidator(CLOCK);

        validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET));

        assertTrue(validator.getClaimErrors().isEmpty());
        assertFalse(validator.hasSignatureError());
        assertNull(validator.getSignatureError());
    }

    @Test
    public void decodedJwtReportsExpirationErrorWithValidSignature() {
        DecodedJWT jwt = signedToken(Instant.EPOCH);
        JwtValidator validator = new JwtValidator(CLOCK);

        validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET));

        assertEquals(List.of("exp"), validator.getClaimErrors().stream().map(ClaimError::getClaim).toList());
        assertFalse(validator.hasSignatureError());
    }

    @Test
    public void decodedJwtReportsBothExpirationAndSignatureErrors() {
        DecodedJWT jwt = signedToken(Instant.EPOCH);
        JwtValidator validator = new JwtValidator(CLOCK);

        validator.validate(jwt, new HS256SignatureContext("wrong secret"));

        assertEquals(List.of("exp"), validator.getClaimErrors().stream().map(ClaimError::getClaim).toList());
        assertTrue(validator.hasSignatureError());
        assertEquals("Signature failed", validator.getSignatureError().getMessage());
    }

    @Test
    public void decodedJwtValidationRetainsFirstSignatureFailure() {
        DecodedJWT jwt = signedToken(NOW.plusSeconds(60));
        JwtValidator validator = new JwtValidator(CLOCK);
        validator.validate(jwt, new HS256SignatureContext("wrong secret"));
        assertTrue(validator.hasSignatureError());
        SignatureError firstError = validator.getSignatureError();

        validator.validate(jwt, new HS256SignatureContext("another wrong secret"));
        assertSame(firstError, validator.getSignatureError());

        validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET));
        assertSame(firstError, validator.getSignatureError());
    }

    @Test
    public void signatureValidatorAcceptsValidSignatureOnExpiredJwt() {
        DecodedJWT jwt = signedToken(Instant.EPOCH);
        JwtSignatureValidator validator = new JwtSignatureValidator();
        assertTrue(validator.validate(jwt, new HS256SignatureContext(SIGNING_SECRET)).isEmpty());
    }

    private static JwtValidator validate(String payload) {
        return new JwtValidator(CLOCK).validateClaims(token(payload));
    }

    private static void assertErrors(String payload, String... claimNames) {
        List<String> actual = validate(payload).getClaimErrors().stream().map(ClaimError::getClaim).toList();
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
