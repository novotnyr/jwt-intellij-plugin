package com.github.novotnyr.idea.jwt.rs256;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.interfaces.RSAPrivateKey;

import static org.junit.Assert.*;

public class RsaUtilsTest {
    private static final String PKCS8_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAJn3q7hnJ7LdjOcn\n" +
            "aYXZWfNOWfXV0ybF3H6VrkqC1WkAjhKG9DuZkdxdxyGlMD6uZ2O3x7hSRGBlPz5m\n" +
            "xFbhUX1GDJL0O2WIe9LFeHLQVvcNQ0TqcbFgqXLb9Tr5PJ2SiJaIE0vpLweaHH1w\n" +
            "i3OX8+liMSbL5sKkv/Sub8luAVUpAgMBAAECgYAfNViVVDNzyfyZjh+RhfD5pFQP\n" +
            "xHu8TDbZYbWmXMatNfOWRcyTmBk8tbV7WGRjEC7cw4+0CNjbQSqEl4gMpDdEAL2+\n" +
            "3dnQkUzYJ0/9neum6rtqPN3Da1L7a9fNe/Qmfko03WK5vlUW0HNd/gq+obWT/I2X\n" +
            "8Khl8XMQjXJAXNBG0QJBAMyCM1q4gJy+FRIJJ5xlDhu7iWdyEBTXT+3BMQyTd1VJ\n" +
            "5XqUEJQ5baBu0x0B7q9tWVe/AN29GxtjpFCJ3bzQQ78CQQDAu8ro+Ripf22bOioE\n" +
            "+cGpQaSwZtd3U+UhetrAxItntJI5+6oBf6OUzxK/56CSucqpXXrVuF26++s4nKek\n" +
            "2IEXAkAXtA+XuELP6AgxKvZ4Xg4gkMAASWwrvjj5aabxujXeoM4EQx0Y0P8v1SiK\n" +
            "+CpeEuKEFRNYoA6AQjBNVW1p8QmpAkEAqBDab3AQZnX9YVhGHhKhIvRgJYd4qtDY\n" +
            "xOo56dE6c1OOE8v2QtpIDlLIWdolNu2W4TvoCoh8C7r0MmjA88nsPwJBAIvLirh1\n" +
            "BXneLLND2Jvc35ZVAZ9R8SG98oFin+z+Jdj2dHEdlmV9BIaksSuKt65xJEeE3ZuQ\n" +
            "jR1dU6n1Tw7w+Lo=\n" +
            "-----END PRIVATE KEY-----";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getPrivateKey() {
        RSAPrivateKey privateKey = RsaUtils.getPrivateKey(PKCS8_PRIVATE_KEY);
        assertNotNull(privateKey);
    }

    @Test
    public void getPrivateKeyFromPkcs8File() throws IOException {
        File file = writeToFile(PKCS8_PRIVATE_KEY);

        assertEquals(RsaUtils.getPrivateKey(PKCS8_PRIVATE_KEY), RsaUtils.getPrivateKey(file));
    }

    @Test
    public void getPrivateKeyFromPkcs1File() throws IOException {
        RSAPrivateKey expectedPrivateKey = RsaUtils.getPrivateKey(PKCS8_PRIVATE_KEY);
        String pkcs1PrivateKey = RsaUtils.toString(expectedPrivateKey);
        assertTrue(pkcs1PrivateKey.startsWith("-----BEGIN RSA PRIVATE KEY-----"));
        File file = writeToFile(pkcs1PrivateKey);

        assertEquals(expectedPrivateKey, RsaUtils.getPrivateKey(file));
    }

    @Test
    public void buildSignatureContextWithPrivateKeyOnly() {
        RS256SignatureContext context = new RS256SignatureContext.Builder()
                .withPrivateKey(PKCS8_PRIVATE_KEY)
                .build();

        assertNotNull(context.getPrivateKey());
        assertNull(context.getPublicKey());
        assertTrue(context.isEmpty());
        assertEquals("", context.getPublicKeyString());
    }

    private File writeToFile(String pem) throws IOException {
        File file = this.temporaryFolder.newFile();
        Files.writeString(file.toPath(), pem);
        return file;
    }
}
