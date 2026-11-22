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

    private final Mode mode;

    private final ClaimPanel claimPanel;

    /**
     * Creates a dialog for viewing, editing, or adding a claim.
     *
     * @param claim the claim to display
     * @param mode determines whether the claim can be edited
     * @param viewOnlyReason an optional explanation shown in {@link Mode#VIEW};
     *                       {@code null} omits the explanation without changing editability
     */
    protected ClaimDialog(NamedClaim<?> claim, Mode mode, @Nullable String viewOnlyReason) {
        super(false);
        this.claim = claim;
        this.mode = mode;
        this.claimPanel = new ClaimPanel(claim, mode, viewOnlyReason);

        init();
    }

    @Override
    protected void doOKAction() {
        if (this.mode != Mode.VIEW) {
            String claimName = this.claimPanel.getClaimName();
            Object claimValue = this.claimPanel.getClaimValue();

            // create copy
            this.claim = ClaimUtils.copyClaim(this.claim, claimName, claimValue);
        }

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
        return this.claim;
    }

    private static class ClaimPanel extends JPanel {
        protected NamedClaim<?> claim;

        protected Mode mode;

        private final @Nullable String viewOnlyReason;

        private final JLabel claimTextLabel = new JLabel("Claim:");

        protected JTextField claimNameTextField = new JTextField();

        protected JLabel claimValueTextLabel = new JLabel("Value:");

        protected AbstractClaimPanel<?, ?> nestedClaimPanel;

        public ClaimPanel(NamedClaim<?> claim, Mode mode, @Nullable String viewOnlyReason) {
            super(new GridBagLayout());

            this.claim = claim;
            this.mode = mode;
            this.viewOnlyReason = viewOnlyReason;

            GridBagConstraints cColumn1 = new GridBagConstraints();
            cColumn1.fill = GridBagConstraints.NONE;
            cColumn1.anchor = GridBagConstraints.LINE_START;
            cColumn1.weightx = 0;
            cColumn1.gridx = 0;
            cColumn1.gridy = 0;
            cColumn1.insets = JBUI.insets(5);

            GridBagConstraints cColumns2 = new GridBagConstraints();
            cColumns2.fill = GridBagConstraints.HORIZONTAL;
            cColumns2.anchor = GridBagConstraints.LINE_START;
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
            cColumns2.fill = GridBagConstraints.BOTH;
            cColumns2.weighty = 1;

            add(this.claimValueTextLabel, cColumn1);
            add(this.nestedClaimPanel = createNestedPanel(), cColumns2);
            this.nestedClaimPanel.setReadOnly(this.mode == Mode.VIEW);

            this.claimNameTextField.setText(claim.getName());
            if (this.mode == Mode.VIEW && this.viewOnlyReason != null) {
                JLabel viewOnlyNote = new JLabel(this.viewOnlyReason);
                viewOnlyNote.setName("jwt.claim.readOnly.note");
                GridBagConstraints noteConstraints = new GridBagConstraints();
                noteConstraints.fill = GridBagConstraints.HORIZONTAL;
                noteConstraints.anchor = GridBagConstraints.LINE_START;
                noteConstraints.weightx = 1;
                noteConstraints.gridx = 0;
                noteConstraints.gridy = 2;
                noteConstraints.gridwidth = 2;
                noteConstraints.insets = JBUI.insets(5);
                add(viewOnlyNote, noteConstraints);
            }
        }

        private @Nullable AbstractClaimPanel<?, ?> createNestedPanel() {
            return switch (this.claim) {
                case DateClaim dateClaim -> new DateClaimPanel(dateClaim);
                case StringClaim stringClaim -> new StringClaimPanel(stringClaim);
                case NumericClaim numericClaim -> new NumericClaimPanel(numericClaim);
                case BooleanClaim booleanClaim -> new BooleanClaimPanel(booleanClaim);
                case RawClaim rawClaim -> new RawClaimPanel(rawClaim);
                case null, default -> null;
            };
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
        NEW, EDIT, VIEW
    }
}
