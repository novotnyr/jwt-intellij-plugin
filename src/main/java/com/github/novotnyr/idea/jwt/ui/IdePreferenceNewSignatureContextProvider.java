package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.EditableKeyPairSignatureContext;
import com.github.novotnyr.idea.jwt.NewSignatureContextProvider;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.ui.preferences.PluginPreferences;
import com.google.common.io.Files;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class IdePreferenceNewSignatureContextProvider implements NewSignatureContextProvider {
    protected final Logger log = Logger.getInstance("#" + IdePreferenceNewSignatureContextProvider.class.getName());

    private final Project project;

    public IdePreferenceNewSignatureContextProvider(Project project) {
        this.project = project;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends SignatureContext> T createSignatureContext(String algorithm) {
        if (!SignatureAlgorithm.RS256.equals(algorithm)) {
            return (T) SignatureContext.EMPTY;
        }
        try {
            if (isEmptyPrivateKeyFile() && isEmptyPublicKeyFile()) {
                return (T) EditableKeyPairSignatureContext.INSTANCE;
            }
            PluginPreferences pluginPreferences = PluginPreferences.getInstance(this.project);

            String publicKey = loadPublicKey(pluginPreferences);
            String privateKey = loadPrivateKey(pluginPreferences);

            RS256SignatureContext.Builder builder = new RS256SignatureContext.Builder();
            if (publicKey != null) {
                builder.withPublicKey(publicKey);
            }
            if (privateKey != null) {
                builder.withPrivateKey(privateKey);
            }
            return (T) builder.build();
        } catch (IOException e) {
            log.warn("Cannot load signature context", e);
            return (T) EditableKeyPairSignatureContext.INSTANCE;
        }
    }

    private PluginPreferences getPreferences() {
        return PluginPreferences.getInstance(this.project);
    }

    private boolean isEmptyPrivateKeyFile() {
        String file = getPreferences().getRs256PrivateKeyFile();
        return isFileUnavailable(file);
    }

    private boolean isEmptyPublicKeyFile() {
        String file = getPreferences().getRs256PublicKeyFile();
        return isFileUnavailable(file);
    }


    private boolean isFileUnavailable(String file) {
        return file == null || file.isEmpty() || !new File(file).isFile();
    }

    private String loadPrivateKey(PluginPreferences pluginPreferences) {
        try {
            return load(pluginPreferences.getRs256PrivateKeyFile());
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Cannot load private key: " + e.getMessage());
            return null;
        }
    }

    private String loadPublicKey(PluginPreferences pluginPreferences) throws IOException {
        try {
            return load(pluginPreferences.getRs256PublicKeyFile());
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Cannot load public key: " + e.getMessage());
            return null;
        }
    }

    private String load(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Empty file path");
        }
        File file = new File(filePath);
        return Files.toString(file, StandardCharsets.UTF_8);
    }
}
