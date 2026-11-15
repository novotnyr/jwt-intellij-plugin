package com.github.novotnyr.idea.jwt.ui.preferences;

import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class PreferencesConfigurable implements Configurable {
    private final Project project;

    private PreferencesSettingsPanel settingsPanel;

    public PreferencesConfigurable(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "JWT";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (this.settingsPanel == null) {
            this.settingsPanel = new PreferencesSettingsPanel();
            this.settingsPanel.getRs256PrivateKeyTextField().addBrowseFolderListener(
                    new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), this.project));
            this.settingsPanel.getRs256PublicKeyTextField().addBrowseFolderListener(
                    new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), this.project));
        }
        return this.settingsPanel.getRootPanel();
    }

    @Override
    public void reset() {
        if (this.settingsPanel == null) {
            return;
        }
        PluginPreferences pluginPreferences = PluginPreferences.getInstance(this.project);
        this.settingsPanel.getRs256PrivateKeyTextField().setText(pluginPreferences.getRs256PrivateKeyFile());
        this.settingsPanel.getRs256PublicKeyTextField().setText(pluginPreferences.getRs256PublicKeyFile());
    }

    @Override
    public void disposeUIResources() {
        this.settingsPanel = null;
    }

    @Override
    public boolean isModified() {
        if (this.settingsPanel == null) {
            return false;
        }

        PluginPreferences pluginPreferences = PluginPreferences.getInstance(this.project);
        String privateKeyFile = this.settingsPanel.getRs256PrivateKeyTextField().getText();
        String publicKeyFile = this.settingsPanel.getRs256PublicKeyTextField().getText();
        return isDifferent(privateKeyFile, pluginPreferences.getRs256PrivateKeyFile())
                || isDifferent(publicKeyFile, pluginPreferences.getRs256PublicKeyFile());
    }

    private static boolean isDifferent(String currentValue, String savedValue) {
        return !StringUtil.notNullize(currentValue).equals(StringUtil.notNullize(savedValue));
    }

    @Override
    public void apply() throws ConfigurationException {
        String publicKeyFile = this.settingsPanel.getRs256PublicKeyTextField().getText();
        String privateKeyFile = this.settingsPanel.getRs256PrivateKeyTextField().getText();
        AtomicReference<SignatureContextException> validationError = new AtomicReference<>();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                validateKeyFiles(publicKeyFile, privateKeyFile);
            } catch (SignatureContextException e) {
                validationError.set(e);
            }
        }, "Validating RSA key files", true, this.project);
        if (!completed) {
            throw new ConfigurationException("RSA key validation was canceled");
        }
        if (validationError.get() != null) {
            throw new ConfigurationException(validationError.get().getMessage());
        }

        PluginPreferences pluginPreferences = PluginPreferences.getInstance(this.project);
        pluginPreferences.updateKeyFiles(publicKeyFile, privateKeyFile);
    }

    void validateKeyFiles(String publicKeyFile, String privateKeyFile) {
        validateRs256PublicKeyFile(publicKeyFile);
        validateRs256PrivateKeyFile(privateKeyFile);
    }

    private static void validateRs256PrivateKeyFile(String path) {
        if (StringUtil.isEmpty(path)) {
            return;
        }
        RsaUtils.getPrivateKey(new File(path));
    }

    private static void validateRs256PublicKeyFile(String path) {
        if (StringUtil.isEmpty(path)) {
            return;
        }
        RsaUtils.getPublicKey(new File(path));
    }

}
