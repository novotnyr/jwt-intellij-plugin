package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.ui.secretpanel.HS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;

public class SecretPanelFactory {
    private static SecretPanelFactory INSTANCE = new SecretPanelFactory();

    private SecretPanelFactory() {
    }

    public static SecretPanelFactory getInstance() {
        return INSTANCE;
    }

    public SecretPanel newSecretPanel(Jwt jwt) {
        if (jwt == null || jwt.getAlgorithm() == null) {
            return new UnrecognitedSecretPanel();
        }

        switch (jwt.getAlgorithm()) {
            case "HS256":
                return new HS256Panel();
            case "RS256":
                return new RS256Panel();
            default:
                return new UnrecognitedSecretPanel();
        }
    }

}
