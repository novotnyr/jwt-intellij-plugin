package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtHelper {
    public static DecodedJWT decodeHmac256(String token) throws JWTVerificationException,
            UnsupportedEncodingException
    {
        return JWT.decode(token);
    }

    public static String unbase64(String base64) {
        byte[] buf = Base64.getDecoder().decode(base64);
        return new String(buf, StandardCharsets.UTF_8);
    }

    public static String prettyUnbase64Json(String base64json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String rawJson = unbase64(base64json);
            Object o = objectMapper.readValue(rawJson, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Illegal JSON", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unreadable JSON", e);
        }
    }
}
