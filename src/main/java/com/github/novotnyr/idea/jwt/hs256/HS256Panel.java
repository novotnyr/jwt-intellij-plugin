package com.github.novotnyr.idea.jwt.hs256;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.ui.SecretPanelDelegatingDocumentAdapter;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import com.github.novotnyr.idea.jwt.core.UnsupportedSignatureContext;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class HS256Panel extends SecretPanel {
    private JPanel root;
    private JTextField secretTextField;

    private SecretPanelDelegatingDocumentAdapter secretTextFieldDocumentListener;

    public void setSecret(String secret) {
        this.secretTextField.setText(secret);
    }

    private String getSecret() {
        return this.secretTextField.getText();
    }

    public JPanel getRoot() {
        return root;
    }

    @Override
    public SignatureContext getSignatureContext() {
        return new HS256SignatureContext(getSecret());
    }

    @Override
    public void setSignatureContext(@Nonnull SignatureContext signatureContext) {
        if (!(signatureContext instanceof HS256SignatureContext)) {
            throw new UnsupportedSignatureContext(signatureContext);
        }
        HS256SignatureContext ctx = (HS256SignatureContext) signatureContext;
        setSecret(ctx.getSecret());
    }

    @Override
    public boolean hasSecret() {
        return getSecret() != null && ! getSecret().isEmpty();
    }

    @Override
    public JComponent getBaloonableComponent() {
        return this.secretTextField;
    }

    @Override
    public void setSignatureContextChangedListener(@Nonnull SignatureContextChangedListener listener) {
        super.setSignatureContextChangedListener(listener);
        this.secretTextFieldDocumentListener = new SecretPanelDelegatingDocumentAdapter(this, listener);
        this.secretTextField.getDocument().addDocumentListener(this.secretTextFieldDocumentListener);
    }

    @Override
    public void removeSignatureContextChangedListener() {
        super.removeSignatureContextChangedListener();
        this.secretTextField.getDocument().removeDocumentListener(this.secretTextFieldDocumentListener);
    }
}
