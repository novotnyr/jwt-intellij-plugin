package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.validation.JwtErrors;

/** A decoded JWT and the claim errors found during decoding. */
public record JsonWebToken(DecodedJWT decodedJwt, JwtErrors errors) {
}
