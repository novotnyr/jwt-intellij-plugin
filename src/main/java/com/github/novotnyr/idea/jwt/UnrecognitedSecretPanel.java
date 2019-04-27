package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class UnrecognitedSecretPanel extends SecretPanel {
    @Override
    public JComponent getRoot() {
        return new JLabel("Unknown");
    }

    @Override
    public boolean hasSecret() {
        return false;
    }

    @Override
    public SignatureContext getSignatureContext() {
        return null;
    }

    @Override
    public void setSignatureContext(SignatureContext signatureContext) {

    }
}
