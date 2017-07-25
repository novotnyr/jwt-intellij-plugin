package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.intellij.openapi.ui.ValidationInfo;

import java.awt.GridLayout;

public class StringClaimPanel extends AbstractClaimPanel<StringClaim, String> {
    public StringClaimPanel(StringClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        setLayout(new GridLayout(1, 0));
        add(this.claimValueTextField);
        this.claimValueTextField.setText(this.value.getValueString());
    }

    @Override
    public String getClaimValue() {
        return this.claimValueTextField.getText();
    }

    @Override
    public ValidationInfo getValidationInfo() {
        if(this.claimValueTextField.getText().isEmpty()) {
            return new ValidationInfo("Value cannot be empty", this.claimValueTextField);
        }
        return null;
    }
}
