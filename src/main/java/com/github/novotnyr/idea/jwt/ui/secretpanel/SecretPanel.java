package com.github.novotnyr.idea.jwt.ui.secretpanel;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.intellij.openapi.Disposable;
import com.intellij.ui.TextAccessor;
import org.jspecify.annotations.NonNull;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

public abstract class SecretPanel implements Disposable {
    private SignatureContextChangedListener signatureContextChangedListener;

    public abstract JComponent getRoot();

    public JComponent getBaloonableComponent() {
        return getRoot();
    }

    public abstract SignatureContext getSignatureContext();

    public abstract JwtStatus getStatus();

    public abstract void setSignatureContext(SignatureContext signatureContext);

    public void setSignatureContextChangedListener(@NonNull SignatureContextChangedListener listener) {
        if (this.signatureContextChangedListener != null) {
            removeSignatureContextChangedListener();
        }
        this.signatureContextChangedListener = listener;
    }

    public void removeSignatureContextChangedListener() {
        this.signatureContextChangedListener = null;
    }

    protected boolean hasText(JTextComponent textComponent) {
        return textComponent.getText() != null && !textComponent.getText().isEmpty();
    }

    protected boolean hasText(TextAccessor textAccessor) {
        return textAccessor.getText() != null && !textAccessor.getText().isEmpty();
    }

    @Override
    public void dispose() {
        removeSignatureContextChangedListener();
    }
}
