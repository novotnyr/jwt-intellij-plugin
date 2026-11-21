package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.validation.JwtClaimsValidator;
import com.github.novotnyr.idea.jwt.validation.JwtErrors;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

import static com.github.novotnyr.idea.jwt.core.JwtJson.MAPPER;

/** Decodes the original JWT claims and reports validation errors alongside them. */
public final class JwtDecoder {
    private final JwtClaimsValidator claimsValidator;

    public JwtDecoder() {
        this(Clock.systemUTC());
    }

    public JwtDecoder(Clock clock) {
        this.claimsValidator = new JwtClaimsValidator(clock);
    }

    /**
     * Decodes the JWT and checks its timestamp claims using this decoder's clock.
     * Pass {@link JsonWebToken#decodedJwt()} and a signature context to
     * {@link JwtValidator#validate(DecodedJWT, SignatureContext)} to verify the signature
     * and reevaluate timestamp claims using the validator's clock.
     *
     * @param token the compact JWT
     * @return the decoded token and its timestamp claim errors
     * @throws JWTDecodeException if the token structure is invalid or its header or payload cannot be decoded
     */
    public JsonWebToken decode(String token) {
        ParsedJwt decoded = parse(token);
        return new JsonWebToken(decoded, new JwtErrors(this.claimsValidator.validate(decoded), null));
    }

    ParsedJwt parse(String token) {
        if (token == null) {
            throw new JWTDecodeException("The token is null");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new JWTDecodeException("The JWT must have three parts");
        }

        ObjectNode header = parseObject(parts[0], "header");
        ObjectNode payload = parsePayload(parts[1]);
        return new ParsedJwt(token, parts[0], parts[1], parts[2], header, payload);
    }

    /** Parses a Base64URL payload segment as JSON with full numeric precision. */
    public ObjectNode parsePayload(String encodedPayload) {
        return parseObject(encodedPayload, "payload");
    }

    private ObjectNode parseObject(String encoded, String partName) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            if (MAPPER.readTree(json) instanceof ObjectNode claims) {
                return claims;
            }
            throw new JWTDecodeException("The JWT " + partName + " must be a JSON object");
        } catch (CharacterCodingException e) {
            throw new JWTDecodeException("The JWT " + partName + " must be UTF-8 encoded", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new JWTDecodeException("Cannot decode the JWT " + partName, e);
        }
    }
}
