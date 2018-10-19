package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.BooleanClaim;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.github.novotnyr.idea.jwt.core.RawClaim;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.ui.claimpanel.AbstractClaimPanel;
import com.github.novotnyr.idea.jwt.ui.claimpanel.BooleanClaimPanel;
import com.github.novotnyr.idea.jwt.ui.claimpanel.DateClaimPanel;
import com.github.novotnyr.idea.jwt.ui.claimpanel.NumericClaimPanel;
import com.github.novotnyr.idea.jwt.ui.claimpanel.RawClaimPanel;
import com.github.novotnyr.idea.jwt.ui.claimpanel.StringClaimPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.NEW;

public class ClaimDialog extends DialogWrapper {
    private NamedClaim<?> claim;

    private ClaimPanel claimPanel;

    protected ClaimDialog(NamedClaim<?> claim, Mode mode) {
        super(false);
        this.claim = claim;
        this.claimPanel = new ClaimPanel(claim, mode);

        init();
    }

    @Override
    protected void doOKAction() {
        String claimName = this.claimPanel.getClaimName();
        Object claimValue = this.claimPanel.getClaimValue();

        // create copy
        this.claim = ClaimUtils.copyClaim(this.claim, claimName, claimValue);

        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.claimPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return this.claimPanel.getValidationInfo();
    }

    public NamedClaim<?> getClaim() {
        return claim;
    }

    private class ClaimPanel extends JPanel {
        protected NamedClaim<?> claim;

        protected Mode mode;

        private JLabel claimTextLabel = new JLabel("Claim:");

        protected JTextField claimNameTextField = new JTextField();

        protected JLabel claimValueTextLabel = new JLabel("Value:");

        protected AbstractClaimPanel<?, ?> nestedClaimPanel;

        public ClaimPanel(NamedClaim<?> claim, Mode mode) {
            super(new GridBagLayout());

            this.claim = claim;
            this.mode = mode;

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
            add(this.claimNameTextField, cColumns2);

            this.claimNameTextField.setEnabled(mode == NEW);

            initBottom(claim, cColumn1, cColumns2);
        }

        protected void initBottom(NamedClaim<?> claim, GridBagConstraints cColumn1, GridBagConstraints cColumns2) {
            cColumn1.gridy = 1;
            cColumns2.gridy = 1;

            add(this.claimValueTextLabel, cColumn1);
            add(this.nestedClaimPanel = createNestedPanel(), cColumns2);

            this.claimNameTextField.setText(claim.getName());
        }

        private AbstractClaimPanel<?, ?> createNestedPanel() {
            if(this.claim instanceof DateClaim) {
                return new DateClaimPanel((DateClaim) this.claim);
            } else if(this.claim instanceof StringClaim) {
                return new StringClaimPanel((StringClaim) this.claim);
            } else if(this.claim instanceof NumericClaim) {
                return new NumericClaimPanel((NumericClaim) this.claim);
            } else if(this.claim instanceof BooleanClaim) {
                return new BooleanClaimPanel((BooleanClaim) this.claim);
            } else if(this.claim instanceof RawClaim) {
                return new RawClaimPanel((RawClaim) this.claim);
            }

            return null;
        }

        public String getClaimName() {
            return this.claimNameTextField.getText();
        }

        public Object getClaimValue() {
            return this.nestedClaimPanel.getClaimValue();
        }

        public ValidationInfo getValidationInfo() {
            return this.nestedClaimPanel.getValidationInfo();
        }
    }

    public enum Mode {
        NEW, EDIT
    }
}
