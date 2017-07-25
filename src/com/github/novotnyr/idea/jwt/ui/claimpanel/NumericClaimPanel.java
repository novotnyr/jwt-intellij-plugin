package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.intellij.openapi.ui.ValidationInfo;

import java.awt.GridLayout;

public class NumericClaimPanel extends AbstractClaimPanel<NumericClaim, Long> {
    public NumericClaimPanel(NumericClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        setLayout(new GridLayout(1, 0));
        add(this.claimValueTextField);
        this.claimValueTextField.setText(this.value.getValueString());
    }

    @Override
    public Long getClaimValue() {
        return Long.valueOf(this.claimValueTextField.getText());
    }

    @Override
    public ValidationInfo getValidationInfo() {
        if(this.claimValueTextField.getText().isEmpty()) {
            return new ValidationInfo("Value cannot be empty", this.claimValueTextField);
        }
        try {
            Long.parseLong(this.claimValueTextField.getText());
        } catch (Exception e) {
            return new ValidationInfo("Value is not a number", this.claimValueTextField);
        }
        return null;
    }
}
