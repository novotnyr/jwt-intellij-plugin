package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContextException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.KeyPairGenerator;

import static org.junit.Assert.*;

public class RsaUtilsTest {
    private static final String PKCS8_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAJn3q7hnJ7LdjOcn
            aYXZWfNOWfXV0ybF3H6VrkqC1WkAjhKG9DuZkdxdxyGlMD6uZ2O3x7hSRGBlPz5m
            xFbhUX1GDJL0O2WIe9LFeHLQVvcNQ0TqcbFgqXLb9Tr5PJ2SiJaIE0vpLweaHH1w
            i3OX8+liMSbL5sKkv/Sub8luAVUpAgMBAAECgYAfNViVVDNzyfyZjh+RhfD5pFQP
            xHu8TDbZYbWmXMatNfOWRcyTmBk8tbV7WGRjEC7cw4+0CNjbQSqEl4gMpDdEAL2+
            3dnQkUzYJ0/9neum6rtqPN3Da1L7a9fNe/Qmfko03WK5vlUW0HNd/gq+obWT/I2X
            8Khl8XMQjXJAXNBG0QJBAMyCM1q4gJy+FRIJJ5xlDhu7iWdyEBTXT+3BMQyTd1VJ
            5XqUEJQ5baBu0x0B7q9tWVe/AN29GxtjpFCJ3bzQQ78CQQDAu8ro+Ripf22bOioE
            +cGpQaSwZtd3U+UhetrAxItntJI5+6oBf6OUzxK/56CSucqpXXrVuF26++s4nKek
            2IEXAkAXtA+XuELP6AgxKvZ4Xg4gkMAASWwrvjj5aabxujXeoM4EQx0Y0P8v1SiK
            +CpeEuKEFRNYoA6AQjBNVW1p8QmpAkEAqBDab3AQZnX9YVhGHhKhIvRgJYd4qtDY
            xOo56dE6c1OOE8v2QtpIDlLIWdolNu2W4TvoCoh8C7r0MmjA88nsPwJBAIvLirh1
            BXneLLND2Jvc35ZVAZ9R8SG98oFin+z+Jdj2dHEdlmV9BIaksSuKt65xJEeE3ZuQ
            jR1dU6n1Tw7w+Lo=
            -----END PRIVATE KEY-----""";

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

    @Test
    public void wrongPublicKeyTypeProducesSignatureContextException() {
        assertThrows(SignatureContextException.class, () -> RsaUtils.getPublicKey(PKCS8_PRIVATE_KEY));
    }

    @Test
    public void malformedPrivateKeyIdentifiesThePrivateField() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        String publicKey = RsaUtils.toString((RSAPublicKey) generator.generateKeyPair().getPublic());

        RsaKeyValidationException error = assertThrows(RsaKeyValidationException.class,
                () -> RsaKeyParser.parse(publicKey, "malformed private key"));
        assertEquals(RsaKeyValidationException.Field.PRIVATE, error.getField());
    }

    @Test
    public void parserAcceptsLiteralNewlineEscapesInBothKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        var pair = generator.generateKeyPair();
        String publicKey = RsaUtils.toString((RSAPublicKey) pair.getPublic()).replace("\n", "\\n");
        String privateKey = RsaUtils.toString((RSAPrivateKey) pair.getPrivate()).replace("\n", "\\n");

        RS256SignatureContext context = RsaKeyParser.parse(publicKey, privateKey);

        assertEquals(pair.getPublic(), context.getPublicKey());
        assertEquals(pair.getPrivate(), context.getPrivateKey());
    }

    private File writeToFile(String pem) throws IOException {
        File file = this.temporaryFolder.newFile();
        Files.writeString(file.toPath(), pem);
        return file;
    }
}
