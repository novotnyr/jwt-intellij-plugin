package com.github.novotnyr.idea.jwt.ui.preferences;

import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.File;

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
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            validateRs256PublicKeyFile();
            validateRs256PrivateKeyFile();

            PluginPreferences pluginPreferences = PluginPreferences.getInstance(this.project);
            pluginPreferences.setRs256PublicKeyFile(this.settingsPanel.getRs256PublicKeyTextField().getText());
            pluginPreferences.setRs256PrivateKeyFile(this.settingsPanel.getRs256PrivateKeyTextField().getText());
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    private void validateRs256PrivateKeyFile() {
        String rs256PrivateKey = this.settingsPanel.getRs256PrivateKeyTextField().getText();
        if (StringUtil.isEmpty(rs256PrivateKey)) {
            return;
        }
        RsaUtils.getPrivateKey(new File(rs256PrivateKey));
    }

    private void validateRs256PublicKeyFile() {
        String rs256PublicKey = this.settingsPanel.getRs256PublicKeyTextField().getText();
        if (StringUtil.isEmpty(rs256PublicKey)) {
            return;
        }
        RsaUtils.getPublicKey(new File(rs256PublicKey));
    }

}
