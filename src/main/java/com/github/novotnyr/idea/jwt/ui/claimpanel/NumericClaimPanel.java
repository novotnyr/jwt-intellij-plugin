package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.intellij.openapi.ui.ValidationInfo;

import java.awt.GridLayout;
import java.math.BigDecimal;

public class NumericClaimPanel extends AbstractClaimPanel<NumericClaim, BigDecimal> {
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
    public BigDecimal getClaimValue() {
        return parseValue();
    }

    @Override
    public ValidationInfo getValidationInfo() {
        if(this.claimValueTextField.getText().isEmpty()) {
            return new ValidationInfo("Value cannot be empty", this.claimValueTextField);
        }
        try {
            parseValue();
        } catch (NumberFormatException e) {
            return new ValidationInfo("Value is not a number", this.claimValueTextField);
        }
        return null;
    }

    private BigDecimal parseValue() {
        return new BigDecimal(this.claimValueTextField.getText().replace(',', '.'));
    }
}
