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

    protected ClaimDialog(NamedClaim<?> claim) {
        super(false);
        this.claim = claim;
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return new ClaimPanel(this.claim);
    }

    public static class ClaimPanel extends JPanel {
        private JLabel claimTextLabel = new JLabel("Claim");

        private JLabel claimNameLabel = new JLabel();

        private JLabel claimValueTextLabel = new JLabel("Value");

        private JTextField claimValueTextField = new JTextField();

        public ClaimPanel(NamedClaim<?> claim) {
            super(new GridBagLayout());

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
            this.claimValueTextField.setText(claim.getValue().toString());
        }
    }
}
