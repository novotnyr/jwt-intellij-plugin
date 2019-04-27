package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import com.github.novotnyr.idea.jwt.validation.SignatureError;
import com.intellij.ui.DocumentAdapter;

import javax.swing.event.DocumentEvent;

public class SecretPanelDelegatingDocumentAdapter extends DocumentAdapter {
    private final SecretPanel secretPanel;

    private final SignatureContextChangedListener delegate;

    public SecretPanelDelegatingDocumentAdapter(SecretPanel secretPanel, SignatureContextChangedListener delegate) {
        this.secretPanel = secretPanel;
        this.delegate = delegate;
    }

    @Override
    protected void textChanged(DocumentEvent event) {
        try {
            this.delegate.onSignatureContextChanged(secretPanel.getSignatureContext());
        } catch (SignatureContextException e) {
            SignatureError signatureError = new SignatureError(e.getMessage());
            secretPanel.notifySignatureErrors(signatureError);
        }
    }
}
