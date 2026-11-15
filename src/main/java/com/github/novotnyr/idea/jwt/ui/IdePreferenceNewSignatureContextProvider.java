package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.EditableKeyPairSignatureContext;
import com.github.novotnyr.idea.jwt.NewSignatureContextProvider;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.ui.preferences.PluginPreferences;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IdePreferenceNewSignatureContextProvider implements NewSignatureContextProvider {
    protected final Logger log = Logger.getInstance("#" + IdePreferenceNewSignatureContextProvider.class.getName());

    private final String publicKeyFile;

    private final String privateKeyFile;

    public IdePreferenceNewSignatureContextProvider(Project project) {
        PluginPreferences preferences = project == null ? null : PluginPreferences.getInstance(project);
        this.publicKeyFile = preferences == null ? null : preferences.getRs256PublicKeyFile();
        this.privateKeyFile = preferences == null ? null : preferences.getRs256PrivateKeyFile();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends SignatureContext> T createSignatureContext(String algorithm) {
        if (!SignatureAlgorithm.RS256.equals(algorithm)) {
            return (T) SignatureContext.EMPTY;
        }
        if (isEmptyPrivateKeyFile() && isEmptyPublicKeyFile()) {
            return (T) EditableKeyPairSignatureContext.INSTANCE;
        }
        String publicKey = loadPublicKey();
        String privateKey = loadPrivateKey();

        RS256SignatureContext.Builder builder = new RS256SignatureContext.Builder();
        if (publicKey != null) {
            builder.withPublicKey(publicKey);
        }
        if (privateKey != null) {
            builder.withPrivateKey(privateKey);
        }
        return (T) builder.build();
    }

    private boolean isEmptyPrivateKeyFile() {
        return isFileUnavailable(this.privateKeyFile);
    }

    private boolean isEmptyPublicKeyFile() {
        return isFileUnavailable(this.publicKeyFile);
    }


    private boolean isFileUnavailable(String file) {
        return file == null || file.isEmpty() || !new File(file).isFile();
    }

    private String loadPrivateKey() {
        try {
            return load(this.privateKeyFile);
        } catch (IOException | IllegalArgumentException e) {
            this.log.warn("Cannot load private key: " + e.getMessage());
            return null;
        }
    }

    private String loadPublicKey() {
        try {
            return load(this.publicKeyFile);
        } catch (IOException | IllegalArgumentException e) {
            this.log.warn("Cannot load public key: " + e.getMessage());
            return null;
        }
    }

    private String load(String file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file path");
        }
        Path path = Paths.get(file);
        return new String(Files.readAllBytes(path));
    }
}
