package com.github.novotnyr.idea.jwt.ui.secretpanel;

import com.github.novotnyr.idea.jwt.SignatureContext;

public class UnsupportedSignatureContext extends RuntimeException {
    public UnsupportedSignatureContext(SignatureContext signatureContext) {
        super("Unsupported signature context " + signatureContext.getClass());
    }
}