package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs384.HS384Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.project.Project;

import static com.github.novotnyr.idea.jwt.SignatureAlgorithm.*;

public final class SecretPanelFactory {
    private SecretPanelFactory() {
    }

    public static SecretPanel create(Project project, String algorithmName, SignatureContext initialSignatureContext) {
        if (algorithmName == null) {
            return new UnrecognizedSecretPanel();
        }
        return switch (algorithmName) {
            case HS256 -> new HS256Panel();
            case HS384 -> new HS384Panel();
            case RS256 -> new RS256Panel(project, initialSignatureContext);
            default -> new UnrecognizedSecretPanel();
        };
    }
}
