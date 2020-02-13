package com.github.novotnyr.idea.jwt.ui.preferences;

import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.io.File;

public class PreferencesConfigurable implements Configurable {
    private final Project project;

    private TextFieldWithBrowseButton rs256PrivateKeyTextField;
    private TextFieldWithBrowseButton rs256PublicKeyTextField;
    private JPanel rootPanel;

    public PreferencesConfigurable(Project project) {
        this.project = project;
        final PluginPreferences pluginPreferences = PluginPreferences.getInstance(project);
        this.rs256PrivateKeyTextField.setText(pluginPreferences.getRs256PrivateKeyFile());
        this.rs256PublicKeyTextField.setText(pluginPreferences.getRs256PublicKeyFile());

        this.rs256PrivateKeyTextField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), project));
        this.rs256PublicKeyTextField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), project));
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "JWT";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return this.rootPanel;
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

            PluginPreferences pluginPreferences = PluginPreferences.getInstance(project);
            pluginPreferences.setRs256PublicKeyFile(this.rs256PublicKeyTextField.getText());
            pluginPreferences.setRs256PrivateKeyFile(this.rs256PrivateKeyTextField.getText());
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    private void validateRs256PrivateKeyFile() {
        String rs256PrivateKey = this.rs256PrivateKeyTextField.getText();
        if (StringUtil.isEmpty(rs256PrivateKey)) {
            return;
        }
        RsaUtils.getPrivateKey(rs256PrivateKey);
    }

    private void validateRs256PublicKeyFile() {
        String rs256PublicKey = this.rs256PublicKeyTextField.getText();
        if (StringUtil.isEmpty(rs256PublicKey)) {
            return;
        }
        RsaUtils.getPublicKey(new File(rs256PublicKey));
    }

}
