package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContextException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.encoders.DecoderException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

    public static RSAPrivateKey getPrivateKey(File privateKeyPemFile) {
        try(FileReader reader = new FileReader(privateKeyPemFile)) {
            return getPrivateKey(reader);
        } catch (FileNotFoundException e) {
            throw new SignatureContextException("Private key PEM file not found: " + privateKeyPemFile, e);
        } catch (IOException e) {
            throw new SignatureContextException("Unable to parse RSA private key from file", e);
        }
    }

    public static RSAPrivateKey getPrivateKey(String privateKeyPem) {
        return getPrivateKey(new StringReader(privateKeyPem));
    }

    public static RSAPrivateKey getPrivateKey(Reader privateKeyPem) {
        try(PEMParser pemParser = new PEMParser(privateKeyPem)) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object pemKeyPairObject = pemParser.readObject();
            if (pemKeyPairObject instanceof SubjectPublicKeyInfo) {
                throw new SignatureContextException("Input is an RSA Public Key, but private key is expected");
            } else if (pemKeyPairObject instanceof PrivateKeyInfo) {
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) pemKeyPairObject;
                return (RSAPrivateKey) new KeyFactorySpi().generatePrivate(privateKeyInfo);
            } else if (pemKeyPairObject instanceof PEMKeyPair) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) pemKeyPairObject;
                KeyPair keyPair = converter.getKeyPair(pemKeyPair);
                return (RSAPrivateKey) keyPair.getPrivate();
            } else {
                throw new SignatureContextException("Unsupported RSA private key type. Is this PKCS#8 or PKCS#1 Private Key?");
            }
        } catch (IOException e) {
            throw new SignatureContextException("Unable to parse RSA private key", e);
        } catch (IllegalArgumentException | NullPointerException | DecoderException e) {
            throw new SignatureContextException("Unable to parse RSA private key. Input is malformed", e);
        }
    }

    public static RSAPublicKey getPublicKey(String publicKeyPem) {
        return getPublicKey(new StringReader(publicKeyPem));
    }

    public static RSAPublicKey getPublicKey(File publicKeyPemFile) {
        try(FileReader publicKeyPemReader = new FileReader(publicKeyPemFile)) {
            return getPublicKey(publicKeyPemReader);
        } catch (FileNotFoundException e) {
            throw new SignatureContextException("Public key PEM file not found: " + publicKeyPemFile, e);
        } catch (IOException e) {
            throw new SignatureContextException("Unable to parse RSA public key from file", e);
        }
    }

    public static RSAPublicKey getPublicKey(Reader reader) {
        try (PEMParser pemParser = new PEMParser(reader)) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object publicKeyInfoObject = pemParser.readObject();
            if (publicKeyInfoObject instanceof PEMKeyPair) {
                throw new SignatureContextException("Input is a private key, but a public key PEM is expected");
            }
            SubjectPublicKeyInfo publicKeyInfo = (SubjectPublicKeyInfo) publicKeyInfoObject;

            return (RSAPublicKey) converter.getPublicKey(publicKeyInfo);
        } catch (IOException e) {
            throw new SignatureContextException("Unable to parse RSA public key", e);
        } catch (IllegalArgumentException | NullPointerException | DecoderException e) {
            throw new SignatureContextException("Unable to parse RSA public key. Input is malformed", e);
        }

    }

    public static String toString(RSAPrivateKey rsaPrivateKey) {
        StringWriter out = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(out)) {
            pemWriter.writeObject(rsaPrivateKey);
            pemWriter.flush();
            return out.toString();
        } catch (IOException e) {
            throw new SignatureContextException("Unable to write RSA private key from string", e);
        }
    }

    public static String toString(RSAPublicKey rsaPublicKey) {
        StringWriter out = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(out)) {
            pemWriter.writeObject(rsaPublicKey);
            pemWriter.flush();
            return out.toString();
        } catch (IOException e) {
            throw new SignatureContextException("Unable to write RSA public key from string", e);
        }
    }

    public static String sanitizeWhitespace(String input) {
        return input.replace("\\n", "\n");
    }
}
