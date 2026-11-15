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
        switch (algorithmName) {
            case HS256:
                return new HS256Panel();
            case HS384:
                return new HS384Panel();
            case RS256:
                return new RS256Panel(project, initialSignatureContext);
            default:
                return new UnrecognizedSecretPanel();
        }
    }
}
