package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.github.novotnyr.idea.jwt.core.UnsupportedSignatureContext;
import com.github.novotnyr.idea.jwt.ui.DelegatingDocumentListener;
import com.github.novotnyr.idea.jwt.ui.FormatOnPasteEditorActionHandler;
import com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import com.github.novotnyr.idea.jwt.validation.SignatureError;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.util.Objects;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.NONE;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.VALID;

public class RS256Panel extends SecretPanel {
    private final SignatureContext initialSignatureContext;
    private Project project;
    private JPanel root;
    private EditorTextField publicKeyEditorTextField;
    private JTextArea privateKeyTextArea;

    private DelegatingDocumentListener<SignatureContextChangedListener> documentListener;

    public RS256Panel(Project project, SignatureContext initialSignatureContext) {
        this.project = project;
        this.initialSignatureContext = initialSignatureContext;
        if (initialSignatureContext instanceof RS256SignatureContext) {
            setSignatureContext(initialSignatureContext);
        }

        publicKeyEditorTextField.setFont(EditorUtil.getEditorFont());

        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler defaultPasteHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_PASTE);
        FormatOnPasteEditorActionHandler pasteHandler = new FormatOnPasteEditorActionHandler(defaultPasteHandler, FormatOnPasteEditorActionHandler::sanitize) {
            @Override
            protected boolean supports(Editor editor) {
                return publicKeyEditorTextField.getEditor() != null && Objects.equals(publicKeyEditorTextField.getEditor(), editor);
            }
        };
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_PASTE, pasteHandler);
    }

    @Override
    public JComponent getRoot() {
        return this.root;
    }

    @Override
    public JwtStatus getStatus() {
        JwtStatus status = NONE;
        if (hasText(this.publicKeyEditorTextField)) {
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
                        .withPublicKey(this.publicKeyEditorTextField.getText())
                        .build();
            case MUTABLE:
                return new RS256SignatureContext.Builder()
                        .withPrivateKey(this.privateKeyTextArea.getText())
                        .withPublicKey(this.publicKeyEditorTextField.getText())
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
        this.publicKeyEditorTextField.setText(ctx.getPublicKeyString());
        this.privateKeyTextArea.setText(ctx.getPrivateKeyString());
    }

    @Override
    public void setSignatureContextChangedListener(@Nonnull SignatureContextChangedListener listener) {
        super.setSignatureContextChangedListener(listener);

        this.documentListener = new DelegatingDocumentListener<SignatureContextChangedListener>(listener) {
            @Override
            protected void onDocumentChanged() {
                try {
                    listener.onSignatureContextChanged(getSignatureContext());
                } catch (SignatureContextException e) {
                    SignatureError signatureError = new SignatureError(e.getMessage());
                    notifySignatureErrors(signatureError);
                }
            }
        };
        this.publicKeyEditorTextField.getDocument().addDocumentListener(this.documentListener);
        this.privateKeyTextArea.getDocument().addDocumentListener(this.documentListener);
    }

    @Override
    public void removeSignatureContextChangedListener() {
        super.removeSignatureContextChangedListener();
        this.publicKeyEditorTextField.getDocument().removeDocumentListener(this.documentListener);
        this.privateKeyTextArea.getDocument().removeDocumentListener(this.documentListener);
    }

    @Override
    public void setProject(@javax.annotation.Nullable Project project) {
        this.project = project;
    }

    private void createUIComponents() {
        this.publicKeyEditorTextField = new EditorTextField("", project, StdFileTypes.PLAIN_TEXT);
        this.publicKeyEditorTextField.setOneLineMode(false);
    }
}
