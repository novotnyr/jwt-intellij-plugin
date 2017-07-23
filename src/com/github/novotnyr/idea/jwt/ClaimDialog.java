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
        String claimName = this.claimPanel.getClaimName();
        String claimValue = this.claimPanel.getClaimValue();

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
        protected NamedClaim<?> claim;

        private JLabel claimTextLabel = new JLabel("Claim:");

        protected JLabel claimNameLabel = new JLabel();

        protected JLabel claimValueTextLabel = new JLabel("Value:");

        protected JTextField claimValueTextField = new JTextField();

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

            initBottom(claim, cColumn1, cColumns2);
        }

        protected void initBottom(NamedClaim<?> claim, GridBagConstraints cColumn1, GridBagConstraints cColumns2) {
            cColumn1.gridy = 1;
            cColumns2.gridy = 1;

            add(this.claimValueTextLabel, cColumn1);
            add(this.claimValueTextField, cColumns2);

            this.claimNameLabel.setText(claim.getName());
            this.claimValueTextField.setText(claim.getValueString());
        }

        public String getClaimName() {
            return this.claimNameLabel.getText();
        }

        public String getClaimValue() {
            return this.claimValueTextField.getText();
        }

        public NamedClaim<?> getClaim() {
            return claim;
        }
    }

    private class DateClaimPanel extends ClaimPanel {
        private JLabel datePreviewLabel = new JLabel();

        public DateClaimPanel(NamedClaim<?> claim) {
            super(claim);
        }

        @Override
        protected void initBottom(NamedClaim<?> claim, GridBagConstraints cColumn1, GridBagConstraints cColumns2) {
            super.initBottom(claim, cColumn1, cColumns2);

            cColumn1.gridy = 2;
            cColumns2.gridy = 2;

            add(this.datePreviewLabel, cColumns2);
            // TODO monitor changes on the textarea
        }


    }
}
