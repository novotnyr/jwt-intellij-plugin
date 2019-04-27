package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContextException;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public abstract class RsaUtils {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static RSAPrivateKey getPrivateKey(String privateKeyPem) {
        try(PEMParser pemParser = new PEMParser(new StringReader(privateKeyPem))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
            KeyPair keyPair = converter.getKeyPair(pemKeyPair);
            return (RSAPrivateKey) keyPair.getPrivate();
        } catch (IOException e) {
            throw new SignatureContextException("Unable to parse RSA private key from string", e);
        }
    }

    public static RSAPublicKey getPublicKey(String publicKeyPem) {
        try(PEMParser pemParser = new PEMParser(new StringReader(publicKeyPem));) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            SubjectPublicKeyInfo publicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            return (RSAPublicKey) converter.getPublicKey(publicKeyInfo);
        } catch (IOException e) {
            throw new SignatureContextException("Unable to parse RSA public key from string", e);
        }
    }

    public static String toString(RSAPrivateKey rsaPrivateKey) {
        StringWriter out = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(out);) {
            pemWriter.writeObject(rsaPrivateKey.getEncoded());
            return out.toString();
        } catch (IOException e) {
            throw new SignatureContextException("Unable to write RSA private key from string", e);
        }
    }

    public static String toString(RSAPublicKey rsaPublicKey) {
        StringWriter out = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(out);) {
            pemWriter.writeObject(rsaPublicKey.getEncoded());
            return out.toString();
        } catch (IOException e) {
            throw new SignatureContextException("Unable to write RSA public key from string", e);
        }
    }
}
