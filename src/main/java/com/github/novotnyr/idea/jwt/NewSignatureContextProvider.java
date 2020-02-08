package com.github.novotnyr.idea.jwt;

public interface NewSignatureContextProvider {
    <T extends SignatureContext> T createSignatureContext(String algorithm);
}
