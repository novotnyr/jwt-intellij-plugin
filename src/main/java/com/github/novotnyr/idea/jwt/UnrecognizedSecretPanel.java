package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SignatureContextChangedListener;
import org.jspecify.annotations.NonNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.NONE;

public class UnrecognizedSecretPanel extends SecretPanel {
    private final JPanel root = new JPanel(new BorderLayout());

    private final JLabel statusLabel = new JLabel("No valid JWT to validate");

    public UnrecognizedSecretPanel() {
        this.statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.root.add(this.statusLabel, BorderLayout.CENTER);
    }

    void showUnsupportedAlgorithm(String algorithm) {
        this.statusLabel.setText(algorithm == null || algorithm.isBlank()
                ? "No valid JWT to validate"
                : "Unsupported algorithm: " + algorithm);
    }

    @Override
    public JComponent getRoot() {
        return this.root;
    }

    @Override
    public JwtStatus getStatus() {
        return NONE;
    }

    @Override
    public SignatureContext getSignatureContext() {
        return SignatureContext.EMPTY;
    }

    @Override
    public void setSignatureContext(SignatureContext signatureContext) {
        // not supported
    }

    @Override
    public void setSignatureContextChangedListener(@NonNull SignatureContextChangedListener listener) {
        super.setSignatureContextChangedListener(listener);
        listener.onSignatureContextChanged();
    }
}
