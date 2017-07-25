package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.core.BooleanClaim;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import java.awt.GridLayout;

public class BooleanClaimPanel extends AbstractClaimPanel<BooleanClaim, Boolean> {
    private ButtonGroup yesNoButtonGroup;

    private JRadioButton trueRadioButton;

    private JRadioButton falseRadioButton;

    public BooleanClaimPanel(BooleanClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        setLayout(new GridLayout(2, 1));

        this.trueRadioButton = new JRadioButton("true");
        this.falseRadioButton = new JRadioButton("false");

        add(this.trueRadioButton);
        add(this.falseRadioButton);

        if(value.getValue()) {
            this.trueRadioButton.setSelected(true);
            this.falseRadioButton.setSelected(false);
        } else {
            this.trueRadioButton.setSelected(false);
            this.falseRadioButton.setSelected(true);
        }

        this.yesNoButtonGroup = new ButtonGroup();
        this.yesNoButtonGroup.add(this.trueRadioButton);
        this.yesNoButtonGroup.add(this.falseRadioButton);
    }

    @Override
    public Boolean getClaimValue() {
        return this.trueRadioButton.isSelected();
    }

}

