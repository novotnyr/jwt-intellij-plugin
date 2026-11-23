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
import com.intellij.ui.EditorNotificationPanel;
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
     * @param viewOnlyReason an optional warning banner shown in {@link Mode#VIEW};
     *                       {@code null} omits the explanation without changing editability
     */
    protected ClaimDialog(NamedClaim<?> claim, Mode mode, @Nullable String viewOnlyReason) {
        super(false);
        this.claim = claim;
        this.mode = mode;
        this.claimPanel = new ClaimPanel(claim, mode, viewOnlyReason);
        setTitle(switch (mode) {
            case NEW -> "Add JWT Claim";
            case EDIT -> "Edit JWT Claim";
            case VIEW -> "View JWT Claim";
        });

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

    private static class ReadOnlyWarningBanner extends EditorNotificationPanel {
        private ReadOnlyWarningBanner() {
            super(JBUI.CurrentTheme.Banner.WARNING_BACKGROUND, Status.Warning);
            this.myLabel.setForeground(JBUI.CurrentTheme.Banner.FOREGROUND);
        }
    }

    private static class ClaimPanel extends JPanel {
        protected NamedClaim<?> claim;

        protected Mode mode;

        private final JLabel claimTextLabel = new JLabel("Claim:");

        protected JTextField claimNameTextField = new JTextField();

        protected JLabel claimValueTextLabel = new JLabel("Value:");

        protected AbstractClaimPanel<?, ?> nestedClaimPanel;

        public ClaimPanel(NamedClaim<?> claim, Mode mode, @Nullable String viewOnlyReason) {
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
            cColumns2.anchor = GridBagConstraints.LINE_START;
            cColumns2.weightx = 1;
            cColumns2.gridx = 1;
            cColumns2.gridy = 0;
            cColumns2.insets = JBUI.insets(5);

            if (mode == Mode.VIEW && viewOnlyReason != null) {
                EditorNotificationPanel viewOnlyBanner = new ReadOnlyWarningBanner();
                viewOnlyBanner.setText(viewOnlyReason);
                viewOnlyBanner.setName("jwt.claim.readOnly.banner");

                GridBagConstraints bannerConstraints = new GridBagConstraints();
                bannerConstraints.fill = GridBagConstraints.HORIZONTAL;
                bannerConstraints.anchor = GridBagConstraints.LINE_START;
                bannerConstraints.weightx = 1;
                bannerConstraints.gridx = 0;
                bannerConstraints.gridy = 0;
                bannerConstraints.gridwidth = 2;
                bannerConstraints.insets = JBUI.insets(5);
                add(viewOnlyBanner, bannerConstraints);

                cColumn1.gridy = 1;
                cColumns2.gridy = 1;
            }

            add(this.claimTextLabel, cColumn1);
            add(this.claimNameTextField, cColumns2);

            this.claimNameTextField.setEditable(mode == NEW);

            initBottom(claim, cColumn1, cColumns2);
        }

        protected void initBottom(NamedClaim<?> claim, GridBagConstraints cColumn1, GridBagConstraints cColumns2) {
            cColumn1.gridy++;
            cColumns2.gridy++;
            cColumns2.fill = GridBagConstraints.BOTH;
            cColumns2.weighty = 1;

            this.nestedClaimPanel = createNestedPanel();
            this.nestedClaimPanel.setReadOnly(this.mode == Mode.VIEW);

            if (this.nestedClaimPanel instanceof DateClaimPanel) {
                this.claimValueTextLabel.setText("Unix timestamp:");
                this.claimValueTextLabel.setLabelFor(this.nestedClaimPanel);
                cColumn1.anchor = GridBagConstraints.BASELINE_LEADING;
                cColumns2.anchor = GridBagConstraints.BASELINE_LEADING;
                cColumns2.fill = GridBagConstraints.HORIZONTAL;
                cColumns2.weighty = 0;
            }

            add(this.claimValueTextLabel, cColumn1);
            add(this.nestedClaimPanel, cColumns2);

            this.claimNameTextField.setText(claim.getName());
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
