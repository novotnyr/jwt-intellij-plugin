package com.github.novotnyr.idea.jwt.ui.preferences;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.JPanel;

final class PreferencesSettingsPanel {
    private TextFieldWithBrowseButton rs256PrivateKeyTextField;
    private TextFieldWithBrowseButton rs256PublicKeyTextField;
    private JPanel rootPanel;

    JPanel getRootPanel() {
        return this.rootPanel;
    }

    TextFieldWithBrowseButton getRs256PrivateKeyTextField() {
        return this.rs256PrivateKeyTextField;
    }

    TextFieldWithBrowseButton getRs256PublicKeyTextField() {
        return this.rs256PublicKeyTextField;
    }
}
