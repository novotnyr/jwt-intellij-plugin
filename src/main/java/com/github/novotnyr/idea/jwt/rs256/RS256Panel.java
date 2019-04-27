package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import com.github.novotnyr.idea.jwt.core.UnsupportedSignatureContext;
import com.github.novotnyr.idea.jwt.validation.SignatureError;
import com.intellij.ui.DocumentAdapter;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;

public class RS256Panel extends SecretPanel {
    private JPanel root;
    private JTextArea publicKeyTextArea;
    private JTextArea privateKeyTextArea;

    private DelegatingDocumentListener documentListener;

    @Override
    public JComponent getRoot() {
        return this.root;
    }

    @Override
    public boolean hasSecret() {
        return hasText(this.privateKeyTextArea) && hasText(this.publicKeyTextArea);
    }

    @Override
    public SignatureContext getSignatureContext() {
        if (!hasSecret()) {
            return SignatureContext.EMPTY;
        }

        return new RS256SignatureContext.Builder()
                .withPrivateKey(this.privateKeyTextArea.getText())
                .withPublicKey(this.publicKeyTextArea.getText())
                .build();
    }

    @Override
    public void setSignatureContext(SignatureContext signatureContext) {
        if (!(signatureContext instanceof RS256SignatureContext)) {
            throw new UnsupportedSignatureContext(signatureContext);
        }
        RS256SignatureContext ctx = (RS256SignatureContext) signatureContext;
        this.publicKeyTextArea.setText(ctx.getPublicKeyString());
        this.privateKeyTextArea.setText(ctx.getPrivateKeyString());
    }

    @Override
    public void setSignatureContextChangedListener(@Nonnull SignatureContextChangedListener listener) {
        super.setSignatureContextChangedListener(listener);

        this.documentListener = new DelegatingDocumentListener(listener);
        this.publicKeyTextArea.getDocument().addDocumentListener(this.documentListener);
        this.privateKeyTextArea.getDocument().addDocumentListener(this.documentListener);
    }

    @Override
    public void removeSignatureContextChangedListener() {
        super.removeSignatureContextChangedListener();
        this.publicKeyTextArea.getDocument().removeDocumentListener(this.documentListener);
        this.privateKeyTextArea.getDocument().removeDocumentListener(this.documentListener);
    }

    private class DelegatingDocumentListener extends DocumentAdapter {
        private final SignatureContextChangedListener delegate;

        private DelegatingDocumentListener(SignatureContextChangedListener delegate) {
            this.delegate = delegate;
        }

        @Override
        protected void textChanged(DocumentEvent event) {
            try {
                this.delegate.onSignatureContextChanged(getSignatureContext());
            } catch (SignatureContextException e) {
                SignatureError signatureError = new SignatureError(e.getMessage());
                notifySignatureErrors(signatureError);
            }
        }
    }

}
