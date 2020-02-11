package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.novotnyr.idea.jwt.SignatureAlgorithm.HS256;
import static com.github.novotnyr.idea.jwt.SignatureAlgorithm.RS256;

public class SecretPanelFactory {
    private static SecretPanelFactory INSTANCE = new SecretPanelFactory();

    private Map<String, SecretPanel> panelCache = new LinkedHashMap<>();

    private SecretPanelFactory() {
    }

    public static SecretPanelFactory getInstance() {
        return INSTANCE;
    }

    @Nonnull
    public SecretPanel getSecretPanel(Project project, @Nonnull String algorithmName, SignatureContext initialSignatureContext) {
        switch (algorithmName) {
            case HS256:
                if (!this.panelCache.containsKey(HS256)) {
                    this.panelCache.put(HS256, new HS256Panel());
                }
                return this.panelCache.get(HS256);
            case RS256:
                if (!this.panelCache.containsKey(RS256)) {
                    this.panelCache.put(RS256, new RS256Panel(project, initialSignatureContext));
                }
                return this.panelCache.get(RS256);
            default:
                return new UnrecognizedSecretPanel();
        }
    }

    public SecretPanel getSecretPanel(Project project, Jwt jwt, SignatureContext initialSignatureContext) {
        if (jwt == null || jwt.getAlgorithm() == null) {
            return new UnrecognizedSecretPanel();
        }

        return getSecretPanel(project, jwt.getAlgorithm(), initialSignatureContext);
    }
}
