package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.intellij.openapi.ui.ValidationInfo;

import javax.swing.JPanel;
import javax.swing.JTextField;

public abstract class AbstractClaimPanel<T extends NamedClaim<?>, V> extends JPanel {
    protected JTextField claimValueTextField = new JTextField();

    protected T value;

    protected ValidationInfo validationInfo;

    protected boolean readOnly;

    public AbstractClaimPanel(T value) {
        this.value = value;
        initialize();
    }

    protected abstract void initialize();

    public abstract V getClaimValue();

    public ValidationInfo getValidationInfo() {
        return this.validationInfo;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        this.claimValueTextField.setEditable(!readOnly);
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
