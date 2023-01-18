package com.github.novotnyr.idea.jwt.hs384;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.UnsupportedSignatureContext;
import com.github.novotnyr.idea.jwt.ui.SecretPanelDelegatingDocumentAdapter;
import com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.NONE;

public class HS384Panel extends SecretPanel {
    private JPanel root;
    private JTextField secretTextField;

    private SecretPanelDelegatingDocumentAdapter secretTextFieldDocumentListener;

    private String getSecret() {
        return this.secretTextField.getText();
    }

    public void setSecret(String secret) {
        this.secretTextField.setText(secret);
    }

    public JPanel getRoot() {
        return root;
    }

    @Override
    public SignatureContext getSignatureContext() {
        return new HS384SignatureContext(getSecret());
    }

    @Override
    public void setSignatureContext(@Nonnull SignatureContext signatureContext) {
        if (!(signatureContext instanceof HS384SignatureContext)) {
            throw new UnsupportedSignatureContext(signatureContext);
        }
        HS384SignatureContext ctx = (HS384SignatureContext) signatureContext;
        setSecret(ctx.getSecret());
    }

    @Override
    public JwtStatus getStatus() {
        boolean hasSecret = getSecret() != null && !getSecret().isEmpty();
        return hasSecret ? MUTABLE : NONE;
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
