package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.StringSecret;
import com.github.novotnyr.idea.jwt.ui.UiUtils;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TextTransferable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

public class JwtPanel extends JPanel {
    private JLabel headerLabel = new JLabel("Header");

    private JwtHeaderTableModel headerTableModel;

    private JBTable headerTable = new JBTable();

    private JLabel payloadLabel = new JLabel("Payload");

    private JwtClaimsTableModel claimsTableModel;

    private JBTable claimsTable = new JBTable();

    private JPanel claimsTablePanel;

    private JLabel verifySignatureLabel = new JLabel("Verify signature with secret:");

    private JTextField secretTextField = new JTextField();

    private JButton validateButton = new JButton("Validate");

    private Jwt jwt;

    public JwtPanel() {
        setLayout(new GridBagLayout());

        GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1;
        cc.weighty = 0;
        cc.anchor = GridBagConstraints.FIRST_LINE_START;
        cc.gridx = 0;
        cc.gridy = 0;
        cc.insets = JBUI.insets(5);
        add(this.headerLabel, cc);
        this.headerLabel.setVisible(false);

        cc.gridy++;
        add(this.headerTable, cc);

        cc.gridy++;
        add(this.payloadLabel, cc);
        this.payloadLabel.setVisible(false);

        cc.gridy++;
        cc.weighty = 1;
        cc.ipady = 50;
        cc.fill = GridBagConstraints.BOTH;
        add(this.claimsTablePanel = configureClaimsTableActions(), cc);
        configureClaimsTablePopup(this.claimsTable);

        cc.gridy++;
        cc.weighty = 0;
        cc.ipady = 0;
        cc.fill = GridBagConstraints.HORIZONTAL;
        add(this.verifySignatureLabel, cc);

        cc.gridy++;
        add(this.secretTextField, cc);

        cc.gridy++;
        add(this.validateButton, cc);
        this.validateButton.setEnabled(false);
        this.validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onValidateButtonClick(e);
            }
        });

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent mouseEvent) {
                return onClaimsTableDoubleClick(mouseEvent);
            };
        }.installOn(this.claimsTable);
    }

    private JPanel configureClaimsTableActions() {
        this.claimsTablePanel = ToolbarDecorator.createDecorator(this.claimsTable)
                .disableAddAction()
                .disableRemoveAction()
                .disableUpDownActions()
                .addExtraAction(new AnActionButton("Copy as JSON", AllIcons.FileTypes.Json) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        onCopyAsJsonActionPerformed(anActionEvent);
                    }
                })
                .createPanel();
        return this.claimsTablePanel;
    }

    private void configureClaimsTablePopup(JBTable table) {
        JPopupMenu popupMenu = new JPopupMenu();
        UiUtils.configureTableRowSelectionOnPopup(popupMenu);

        JMenuItem copyValueMenuItem = new JMenuItem("Copy value (as string)");
        copyValueMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JwtPanel.this.onCopyValueMenuItemActionPerformed(e);
            }
        });
        popupMenu.add(copyValueMenuItem);

        JMenuItem copyAsKeyAndValueMenuItem = new JMenuItem(new AbstractAction("Copy value (as key=value)") {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCopyKeyAndValueMenuItemActionPerformed(e);
            }
        });
        popupMenu.add(copyAsKeyAndValueMenuItem);

        this.claimsTable.setComponentPopupMenu(popupMenu);
    }


    private void onCopyAsJsonActionPerformed(AnActionEvent anActionEvent) {
        TextTransferable textTransferable = new TextTransferable(JwtHelper.prettyUnbase64Json(this.jwt.getPayloadString()));
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }


    public void onCopyValueMenuItemActionPerformed(ActionEvent e) {
        int selectedRowIndex = this.claimsTable.getSelectedRow();
        Object claimValue = this.claimsTableModel.getValueAt(selectedRowIndex, 1);
        TextTransferable textTransferable = new TextTransferable(claimValue.toString());
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    public void onCopyKeyAndValueMenuItemActionPerformed(ActionEvent e) {
        int selectedRowIndex = this.claimsTable.getSelectedRow();
        Object claimName = this.claimsTableModel.getValueAt(selectedRowIndex, 0);
        Object claimValue = this.claimsTableModel.getValueAt(selectedRowIndex, 1);
        TextTransferable textTransferable = new TextTransferable(claimName + "=" + claimValue.toString());

        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    private void onValidateButtonClick(ActionEvent e) {
        JwtValidator jwtValidator = new JwtValidator();

        String secret = this.secretTextField.getText();
        jwtValidator.validate(this.jwt, secret);
        this.claimsTableModel.setClaimErrors(jwtValidator.getClaimErrors());
        if(jwtValidator.hasSignatureError()) {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(jwtValidator.getSignatureError().getMessage(), MessageType.ERROR, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getNorthWestOf(this.secretTextField),
                            Balloon.Position.atRight);
        } else {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder("Signature is valid", MessageType.INFO, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getNorthWestOf(this.secretTextField),
                            Balloon.Position.atRight);
        }
    }

    private boolean onClaimsTableDoubleClick(MouseEvent mouseEvent) {
        if(!hasSecret()) {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder("Cannot edit claims when a secret is empty", MessageType.WARNING, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getNorthWestOf(this.secretTextField),
                            Balloon.Position.atRight);
            return true;
        }

        int selectedRow = claimsTable.rowAtPoint(mouseEvent.getPoint());
        NamedClaim<?> claim = claimsTableModel.getClaimAt(selectedRow);
        ClaimDialog claimDialog = new ClaimDialog(claim);
        if(claimDialog.showAndGet()) {
            NamedClaim<?> updatedClaim = claimDialog.getClaim();
            this.jwt.setSigningCredentials(new StringSecret(getSecret()));
            this.jwt.setPayloadClaim(updatedClaim);
            Jwt oldJwt = this.jwt;
            setJwt(this.jwt);
            firePropertyChange("jwt", null, this.jwt);
        }

        return true;
    }

   public void setJwt(Jwt jwt) {
        this.jwt = jwt;

        this.headerLabel.setVisible(true);
        this.payloadLabel.setVisible(true);
        this.validateButton.setEnabled(true);

        this.headerTableModel = new JwtHeaderTableModel(jwt);
        this.headerTable.setModel(this.headerTableModel);

        this.claimsTableModel = new JwtClaimsTableModel(jwt);
        this.claimsTableModel.setClaimErrors(validateClaims(jwt));
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.setDefaultRenderer(Object.class, this.claimsTableModel);

    }

    private List<ClaimError> validateClaims(Jwt jwt) {
        return new JwtValidator().validateClaims(jwt).getClaimErrors();
    }

    public String getSecret() {
        return this.secretTextField.getText();
    }

    public boolean hasSecret() {
        return getSecret() != null && ! getSecret().isEmpty();
    }

    public JTextField getSecretTextField() {
        return secretTextField;
    }
}
