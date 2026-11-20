package com.github.novotnyr.idea.jwt.ui.secretpanel;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.github.novotnyr.idea.jwt.validation.SignatureError;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.TextAccessor;
import com.intellij.ui.awt.RelativePoint;
import org.jspecify.annotations.NonNull;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

public abstract class SecretPanel implements Disposable {
    private SignatureContextChangedListener signatureContextChangedListener;

    public abstract JComponent getRoot();

    public void notifySignatureErrors(SignatureError signatureError) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(signatureError.message(), MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(getBaloonableComponent()),
                        Balloon.Position.atRight);
    }

    public void notifySignatureValid() {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("Signature is valid", MessageType.INFO, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(getBaloonableComponent()),
                        Balloon.Position.atRight);
    }

    public void notifyEmptySignature() {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("Cannot edit claims when a secret or keypair is not provided", MessageType.WARNING, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(getBaloonableComponent()),
                        Balloon.Position.atRight);
    }


    public void notifySecurityContextException(SignatureContextException e) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("There is an issue with secret, private key or public key: " + e.getMessage(), MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(getBaloonableComponent()),
                        Balloon.Position.atRight);
    }

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
