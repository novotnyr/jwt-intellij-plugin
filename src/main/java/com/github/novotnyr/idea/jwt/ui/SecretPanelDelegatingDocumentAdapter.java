package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import com.intellij.ui.DocumentAdapter;

import javax.swing.event.DocumentEvent;

public class SecretPanelDelegatingDocumentAdapter extends DocumentAdapter {
    private final SignatureContextChangedListener delegate;

    public SecretPanelDelegatingDocumentAdapter(SignatureContextChangedListener delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void textChanged(DocumentEvent event) {
        this.delegate.onSignatureContextChanged();
    }
}
