package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.validation.JwtErrors;

/**
 * A decoded JWT and the timestamp claim errors found during decoding.
 *
 * @param decodedJwt the decoded token with its original encoded segments
 * @param errors timestamp claim errors evaluated using the decoder's clock.
 *               For results from {@link JwtDecoder#decode(String)}, the signature is unchecked
 *               and {@link JwtErrors#signatureError()} is {@code null}.
 */
public record JsonWebToken(DecodedJWT decodedJwt, JwtErrors errors) {
}
