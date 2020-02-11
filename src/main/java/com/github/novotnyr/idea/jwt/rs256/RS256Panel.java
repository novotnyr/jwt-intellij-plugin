package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.github.novotnyr.idea.jwt.core.UnsupportedSignatureContext;
import com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import com.github.novotnyr.idea.jwt.validation.SignatureError;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EditorTextField;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.NONE;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.VALID;

public class RS256Panel extends SecretPanel {
    private JPanel root;
    private EditorTextField publicKeyTextField;
    private JTextArea privateKeyTextArea;

    private DelegatingDocumentListener documentListener;

    public RS256Panel(SignatureContext initialSignatureContext) {
        if (initialSignatureContext instanceof RS256SignatureContext) {
            setSignatureContext(initialSignatureContext);
        }
        publicKeyTextField.setFont(EditorUtil.getEditorFont());
    }

    @Override
    public JComponent getRoot() {
        return this.root;
    }

    @Override
    public JwtStatus getStatus() {
        JwtStatus status = NONE;
        if (hasText(this.publicKeyTextField)) {
            status = VALID;
            if (hasText(this.privateKeyTextArea)) {
                status = MUTABLE;
            }
        }
        return status;
    }

    @Override
    public SignatureContext getSignatureContext() {
        switch (getStatus()) {
            case VALID:
                return new RS256SignatureContext.Builder()
                        .withPublicKey(this.publicKeyTextField.getText())
                        .build();
            case MUTABLE:
                return new RS256SignatureContext.Builder()
                        .withPrivateKey(this.privateKeyTextArea.getText())
                        .withPublicKey(this.publicKeyTextField.getText())
                        .build();
            default:
                return SignatureContext.EMPTY;
        }
    }

    @Override
    public void setSignatureContext(SignatureContext signatureContext) {
        if (!(signatureContext instanceof RS256SignatureContext)) {
            throw new UnsupportedSignatureContext(signatureContext);
        }
        RS256SignatureContext ctx = (RS256SignatureContext) signatureContext;
        this.publicKeyTextField.setText(ctx.getPublicKeyString());
        this.privateKeyTextArea.setText(ctx.getPrivateKeyString());
    }

    @Override
    public void setSignatureContextChangedListener(@Nonnull SignatureContextChangedListener listener) {
        super.setSignatureContextChangedListener(listener);

        this.documentListener = new DelegatingDocumentListener(listener);
        this.publicKeyTextField.getDocument().addDocumentListener(this.documentListener);
        this.privateKeyTextArea.getDocument().addDocumentListener(this.documentListener);
    }

    @Override
    public void removeSignatureContextChangedListener() {
        super.removeSignatureContextChangedListener();
        this.publicKeyTextField.getDocument().removeDocumentListener(this.documentListener);
        this.privateKeyTextArea.getDocument().removeDocumentListener(this.documentListener);
    }

    private void createUIComponents() {
        this.publicKeyTextField = new EditorTextField();
        this.publicKeyTextField.setOneLineMode(false);
    }

    private class DelegatingDocumentListener extends DocumentAdapter implements DocumentListener {
        private final SignatureContextChangedListener delegate;

        private DelegatingDocumentListener(SignatureContextChangedListener delegate) {
            this.delegate = delegate;
        }

        @Override
        protected void textChanged(DocumentEvent event) {
            onDocumentChanged();
        }


        @Override
        public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
            onDocumentChanged();
        }
        private void onDocumentChanged() {
            try {
                this.delegate.onSignatureContextChanged(getSignatureContext());
            } catch (SignatureContextException e) {
                SignatureError signatureError = new SignatureError(e.getMessage());
                notifySignatureErrors(signatureError);
            }
        }

    }

}
