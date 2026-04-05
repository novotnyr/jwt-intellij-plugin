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
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Objects;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.*;

public class RS256Panel extends SecretPanel {
    private Project project;
    private final BorderLayoutPanel root;
    private final EditorTextField publicKeyEditorTextField;
    private final EditorTextField privateKeyEditorTextField;

    private DelegatingDocumentListener<SignatureContextChangedListener> documentListener;

    public RS256Panel(Project project, SignatureContext initialSignatureContext) {
        this.project = project;

        this.publicKeyEditorTextField = new EditorTextField("", this.project, FileTypes.PLAIN_TEXT);
        this.publicKeyEditorTextField.setOneLineMode(false);

        this.privateKeyEditorTextField = new EditorTextField("", this.project, FileTypes.PLAIN_TEXT);
        this.privateKeyEditorTextField.setOneLineMode(false);

        this.root = createUI();

        if (initialSignatureContext instanceof RS256SignatureContext) {
            setSignatureContext(initialSignatureContext);
        }

        this.publicKeyEditorTextField.setFont(EditorUtil.getEditorFont());
        this.privateKeyEditorTextField.setFont(EditorUtil.getEditorFont());

        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler defaultPasteHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_PASTE);
        FormatOnPasteEditorActionHandler pasteHandler = new FormatOnPasteEditorActionHandler(defaultPasteHandler, FormatOnPasteEditorActionHandler::sanitize) {
            @Override
            protected boolean supports(Editor editor) {
                return (RS256Panel.this.publicKeyEditorTextField.getEditor() != null && Objects.equals(RS256Panel.this.publicKeyEditorTextField.getEditor(), editor))
                        || (RS256Panel.this.privateKeyEditorTextField.getEditor() != null && Objects.equals(RS256Panel.this.privateKeyEditorTextField.getEditor(), editor));
            }
        };
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_PASTE, pasteHandler);
    }

    private BorderLayoutPanel createUI() {
        JBScrollPane publicKeyScrollPane = new JBScrollPane(this.publicKeyEditorTextField);
        publicKeyScrollPane.setBorder(IdeBorderFactory.createTitledBorder("Public Key", false));

        JBScrollPane privateKeyScrollPane = new JBScrollPane(this.privateKeyEditorTextField);
        privateKeyScrollPane.setBorder(IdeBorderFactory.createTitledBorder("RSA Private Key", false));

        JBSplitter splitter = new JBSplitter(false, 0.5f);
        splitter.setFirstComponent(publicKeyScrollPane);
        splitter.setSecondComponent(privateKeyScrollPane);

        return new BorderLayoutPanel().addToCenter(splitter);
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
            if (hasText(this.privateKeyEditorTextField)) {
                status = MUTABLE;
            }
        }
        return status;
    }

    @Override
    public SignatureContext getSignatureContext() {
        return switch (getStatus()) {
            case VALID -> new RS256SignatureContext.Builder()
                    .withPublicKey(this.publicKeyEditorTextField.getText())
                    .build();
            case MUTABLE -> new RS256SignatureContext.Builder()
                    .withPrivateKey(this.privateKeyEditorTextField.getText())
                    .withPublicKey(this.publicKeyEditorTextField.getText())
                    .build();
            default -> SignatureContext.EMPTY;
        };
    }

    @Override
    public void setSignatureContext(SignatureContext signatureContext) {
        if (!(signatureContext instanceof RS256SignatureContext)) {
            throw new UnsupportedSignatureContext(signatureContext);
        }
        RS256SignatureContext ctx = (RS256SignatureContext) signatureContext;
        this.publicKeyEditorTextField.setText(ctx.getPublicKeyString());
        this.privateKeyEditorTextField.setText(ctx.getPrivateKeyString());
    }

    @Override
    public void setSignatureContextChangedListener(@NonNull SignatureContextChangedListener listener) {
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
        this.privateKeyEditorTextField.getDocument().addDocumentListener(this.documentListener);
    }

    @Override
    public void removeSignatureContextChangedListener() {
        super.removeSignatureContextChangedListener();
        this.publicKeyEditorTextField.getDocument().removeDocumentListener(this.documentListener);
        this.privateKeyEditorTextField.getDocument().removeDocumentListener(this.documentListener);
    }

    @Override
    public void setProject(@Nullable Project project) {
        this.project = project;
    }
}
