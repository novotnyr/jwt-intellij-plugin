package com.github.novotnyr.idea.jwt.ui.preferences;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

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
        PluginPreferences pluginPreferences = PluginPreferences.getInstance(project);
        pluginPreferences.setRs256PublicKeyFile(this.rs256PublicKeyTextField.getText());
        pluginPreferences.setRs256PrivateKeyFile(this.rs256PrivateKeyTextField.getText());
    }

}
