package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class ClaimDialog extends DialogWrapper {
    private NamedClaim<?> claim;

    private ClaimPanel claimPanel;

    protected ClaimDialog(NamedClaim<?> claim) {
        super(false);
        this.claim = claim;
        this.claimPanel = new ClaimPanel(claim);

        init();
    }

    @Override
    protected void doOKAction() {
        String claimName = this.claimPanel.claimNameLabel.getText();
        String claimValue = this.claimPanel.claimValueTextField.getText();

        // create copy
        this.claim = ClaimUtils.newClaim(claimName, claimValue);

        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.claimPanel;
    }

    public NamedClaim<?> getClaim() {
        return claim;
    }

    private class ClaimPanel extends JPanel {
        private NamedClaim<?> claim;

        private JLabel claimTextLabel = new JLabel("Claim");

        private JLabel claimNameLabel = new JLabel();

        private JLabel claimValueTextLabel = new JLabel("Value");

        private JTextField claimValueTextField = new JTextField();

        public ClaimPanel(NamedClaim<?> claim) {
            super(new GridBagLayout());

            this.claim = claim;

            GridBagConstraints cColumn1 = new GridBagConstraints();
            cColumn1.fill = GridBagConstraints.NONE;
            cColumn1.anchor = GridBagConstraints.LINE_START;
            cColumn1.weightx = 0;
            cColumn1.gridx = 0;
            cColumn1.gridy = 0;
            cColumn1.insets = JBUI.insets(5);

            GridBagConstraints cColumns2 = new GridBagConstraints();
            cColumns2.fill = GridBagConstraints.HORIZONTAL;
            cColumn1.anchor = GridBagConstraints.LINE_START;
            cColumns2.weightx = 1;
            cColumns2.gridx = 1;
            cColumns2.gridy = 0;
            cColumns2.insets = JBUI.insets(5);

            add(this.claimTextLabel, cColumn1);
            add(this.claimNameLabel, cColumns2);

            cColumn1.gridy = 1;
            cColumns2.gridy = 1;

            add(this.claimValueTextLabel, cColumn1);
            add(this.claimValueTextField, cColumns2);

            this.claimNameLabel.setText(claim.getName());
            this.claimValueTextField.setText(claim.getValueString());
        }

        public NamedClaim<?> getClaim() {
            return claim;
        }
    }
}
