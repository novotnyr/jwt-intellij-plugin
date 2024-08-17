package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.JWT;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class JwtTest {
    @Test
    @Ignore
    public void test() throws Exception {
        Jwt jwt = new Jwt();
        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setAlgorithm("HS256");
        jwt.setPayloadClaim(new StringClaim("wai", "test"));

        String s = jwt.toString();
        Assert.assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWkiOiJ0ZXN0In0.Rsv4ude3yrQWdnusO4j_g4Fkg2twwNcso1FEOixnKtk", s);
    }

    @Test
    @Ignore
    public void testDecode() throws Exception {
        String jwtString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWkiOiJ0ZXN0In0.Rsv4ude3yrQWdnusO4j_g4Fkg2twwNcso1FEOixnKtk";

        Jwt jwt = new Jwt(jwtString);
        List<NamedClaim<?>> payloadClaims = jwt.getPayloadClaims();
        Assert.assertEquals(1, payloadClaims.size());

        Assert.assertEquals("wai", payloadClaims.get(0).getName());
        Assert.assertEquals("test", payloadClaims.get(0).getValue());

    }

    @Test
    @Ignore
    public void testDate() throws Exception {
        Jwt jwt = new Jwt();
        jwt.setSignatureContext(new HS256SignatureContext("secret"));
        jwt.setAlgorithm("HS256");
        jwt.setPayloadClaim(new DateClaim("dat", new Date(1500814917000L)));

        String s = jwt.toString();
        Assert.assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkYXQiOjE1MDA4MTQ5MTd9.6f43dfop34oMDNNxLVNAGHraf86Xsox95N6wCDiSLDo", s);

        Date dat = JWT.decode(s).getClaim("dat").asDate();
        Assert.assertEquals(1500814917000L, dat.getTime());
    }

}