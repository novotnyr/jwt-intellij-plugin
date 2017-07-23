package com.github.novotnyr.idea.jwt.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JwtTest {
    @Test
    public void test() throws Exception {
        Jwt jwt = new Jwt();
        jwt.setSigningCredentials(new StringSecret("secret"));
        jwt.setAlgorithm("HS256");
        jwt.setPayloadClaim(new StringClaim("wai", "test"));

        String s = jwt.toString();
        Assert.assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWkiOiJ0ZXN0In0.Rsv4ude3yrQWdnusO4j_g4Fkg2twwNcso1FEOixnKtk", s);
    }

    @Test
    public void testDecode() throws Exception {
        String jwtString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWkiOiJ0ZXN0In0.Rsv4ude3yrQWdnusO4j_g4Fkg2twwNcso1FEOixnKtk";

        Jwt jwt = new Jwt(jwtString);
        List<NamedClaim<?>> payloadClaims = jwt.getPayloadClaims();
        Assert.assertEquals(1, payloadClaims.size());

        Assert.assertEquals("wai", payloadClaims.get(0).getName());
        Assert.assertEquals("test", payloadClaims.get(0).getValue());

    }
}