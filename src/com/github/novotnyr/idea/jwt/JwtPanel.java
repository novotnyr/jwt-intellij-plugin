package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.action.AbstractActionButtonController;
import com.github.novotnyr.idea.jwt.action.AddClaimActionButtonController;
import com.github.novotnyr.idea.jwt.action.EditClaimActionButtonUpdater;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.SigningCredentials;
import com.github.novotnyr.idea.jwt.core.StringSecret;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.github.novotnyr.idea.jwt.ui.ClaimTableTransferHandler;
import com.github.novotnyr.idea.jwt.ui.UiUtils;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TextTransferable;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

public class JwtPanel extends JPanel implements DataProvider {
    private JLabel headerLabel = new JLabel("Header");

    private JwtHeaderTableModel headerTableModel;

    private JBTable headerTable = new JBTable();

    private JLabel payloadLabel = new JLabel("Payload");

    private JwtClaimsTableModel claimsTableModel;

    private JBTable claimsTable = new JBTable();

    private JPanel claimsTablePanel;

    private JLabel signatureLabel = new JLabel("Sign/verify signature with secret:");

    private JTextField secretTextField = new JTextField();

    private JButton validateButton = new JButton("Validate");

    private Jwt jwt;

    private AddClaimActionButtonController addClaimActionButtonController;

    private EditClaimActionButtonUpdater editClaimActionButtonUpdater;

    private AbstractActionButtonController removeClaimActionButtonController;

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
        this.claimsTable.setName(Constants.CLAIMS_TABLE_NAME);
        add(this.claimsTablePanel = configureClaimsTableActions(), cc);
        initializeClaimsTableModel(this.claimsTable);
        configureClaimsTablePopup(this.claimsTable);
        configureClipboardCopy(this.claimsTable);

        cc.gridy++;
        cc.weighty = 0;
        cc.ipady = 0;
        cc.fill = GridBagConstraints.HORIZONTAL;
        add(this.signatureLabel, cc);

        cc.gridy++;
        add(this.secretTextField, cc);
        configureSecretTextFieldListeners(this.secretTextField);

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
            }
        }.installOn(this.claimsTable);
    }

    private void initializeClaimsTableModel(JBTable claimsTable) {
        this.claimsTableModel = new JwtClaimsTableModel(Jwt.EMPTY);
        claimsTable.setModel(this.claimsTableModel);
    }


    private void configureClipboardCopy(JBTable claimsTable) {
        this.claimsTable.setTransferHandler(new ClaimTableTransferHandler());
    }

    private JPanel configureClaimsTableActions() {
        this.addClaimActionButtonController = new AddClaimActionButtonController() {
            @Override
            public void onClaimTypeSelected(DataTypeRegistry.DataType dataType) {
                onNewAction(dataType);
            }
        };

        this.editClaimActionButtonUpdater = new EditClaimActionButtonUpdater();

        this.removeClaimActionButtonController = new AbstractActionButtonController() {
            @Override
            public void run(AnActionButton button) {
                onRemoveClaim();
            }

            @Override
            public boolean isEnabled(AnActionEvent anActionEvent) {
                return isRemoveClaimActionEnabled();
            }
        };

        this.claimsTablePanel = ToolbarDecorator.createDecorator(this.claimsTable)
                .disableUpDownActions()
                .addExtraAction(new AnActionButton("Copy as JSON", AllIcons.FileTypes.Json) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        onCopyAsJsonActionPerformed(anActionEvent);
                    }
                })
                .setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton anActionButton) {
                        onEditAction(anActionButton);
                    }
                })
                .setEditActionUpdater(this.editClaimActionButtonUpdater)
                .setAddAction(this.addClaimActionButtonController)
                .setAddActionUpdater(this.addClaimActionButtonController)
                .setRemoveAction(this.removeClaimActionButtonController)
                .setRemoveActionUpdater(this.removeClaimActionButtonController)
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

    private void configureSecretTextFieldListeners(JTextField secretTextField) {
        secretTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                onSecretTextFieldTextChanged(documentEvent);
            }
        });
    }

    private void editClaimAtRow(int row) {
        NamedClaim<?> claim = this.claimsTableModel.getClaimAt(row);
        showClaimDialog(claim, ClaimDialog.Mode.EDIT);
    }


    private void onCopyAsJsonActionPerformed(AnActionEvent anActionEvent) {
        if (this.jwt == null) {
            return;
        }
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

        int selectedRow = this.claimsTable.rowAtPoint(mouseEvent.getPoint());
        if(selectedRow < 0) {
            return true;
        }
        editClaimAtRow(selectedRow);

        return true;
    }

    private void onEditAction(AnActionButton anActionButton) {
        int selectedRow = this.claimsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        editClaimAtRow(selectedRow);
    }

    private void onNewAction(DataTypeRegistry.DataType dataType) {
        NamedClaim<?> claim = ClaimUtils.newEmptyClaim(dataType);
        showClaimDialog(claim, ClaimDialog.Mode.NEW);
    }

    private void onRemoveClaim() {
        int selectedRow = this.claimsTable.getSelectedRow();
        NamedClaim<?> claim = this.claimsTableModel.getClaimAt(selectedRow);
        this.jwt.setSigningCredentials(new StringSecret(getSecret()));
        this.jwt.removeClaim(claim.getName());

        refreshJwt();
    }


    private void showClaimDialog(NamedClaim<?> claim, ClaimDialog.Mode mode) {
        ClaimDialog claimDialog = new ClaimDialog(claim, mode);
        if(claimDialog.showAndGet()) {
            NamedClaim<?> updatedClaim = claimDialog.getClaim();
            this.jwt.setSigningCredentials(new StringSecret(getSecret()));
            this.jwt.setPayloadClaim(updatedClaim);
            refreshJwt();
        }
    }

    private void onSecretTextFieldTextChanged(DocumentEvent documentEvent) {
        final boolean secretIsPresent = documentEvent.getDocument().getLength() > 0;

        refreshClaimsTableControllers(secretIsPresent);
    }

    private void refreshClaimsTableControllers(final boolean secretIsPresent) {
        DataContext dataContext = new DataContext() {
            @Nullable
            @Override
            public Object getData(String dataId) {
                if (Constants.DataKeys.SECRET_IS_PRESENT.is(dataId)) {
                    return secretIsPresent;
                }
                if (Constants.DataKeys.JWT.is(dataId)) {
                    return JwtPanel.this.jwt;
                }
                return null;
            }
        };

        AnActionEvent event = AnActionEvent.createFromDataContext("place", null, dataContext);
        this.addClaimActionButtonController.isEnabled(event);
        this.editClaimActionButtonUpdater.isEnabled(event);
    }

    @Nullable
    @Override
    public Object getData(String dataId) {
        if (Constants.DataKeys.SECRET.is(dataId)) {
            return this.getSecret();
        }
        if (PlatformDataKeys.SELECTED_ITEM.is(dataId)) {
            int selectedRow = this.claimsTable.getSelectedRow();
            if (selectedRow < 0) {
                return null;
            }
            return this.claimsTableModel.getClaimAt(selectedRow);
        }

        return null;
    }

    private void refreshJwt() {
        setJwt(this.jwt);
        firePropertyChange("jwt", null, this.jwt);
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
        this.headerTableModel = new JwtHeaderTableModel(jwt);
        this.headerTable.setModel(this.headerTableModel);

        if (Jwt.EMPTY.equals(jwt)) {
            this.headerLabel.setVisible(false);
            this.payloadLabel.setVisible(false);
            this.validateButton.setEnabled(false);
            refreshClaimsTableControllers(hasSecret());
        } else {
            this.headerLabel.setVisible(true);
            this.payloadLabel.setVisible(true);
            this.validateButton.setEnabled(true);
            refreshClaimsTableControllers(hasSecret());
        }

        this.claimsTableModel = new JwtClaimsTableModel(jwt);
        this.claimsTableModel.setClaimErrors(validateClaims(jwt));
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.setDefaultRenderer(Object.class, this.claimsTableModel);

    }

    private List<ClaimError> validateClaims(Jwt jwt) {
        if (Jwt.EMPTY.equals(jwt)) {
            return Collections.emptyList();
        }
        return new JwtValidator().validateClaims(jwt).getClaimErrors();
    }

    public String getSecret() {
        return this.secretTextField.getText();
    }

    public void setSecret(String secret) {
        this.secretTextField.setText(secret);
    }

    public void setSigningCredentials(SigningCredentials signingCredentials) {
        if (signingCredentials instanceof StringSecret) {
            StringSecret stringSecret = (StringSecret) signingCredentials;
            this.secretTextField.setText(stringSecret.getSecret());
        }
    }


    public boolean hasSecret() {
        return getSecret() != null && ! getSecret().isEmpty();
    }

    public JTextField getSecretTextField() {
        return secretTextField;
    }

    private boolean isRemoveClaimActionEnabled() {
        return hasSecret() && claimsTable.getSelectedRows().length > 0;
    }

    public void reset() {
        setJwt(Jwt.EMPTY);
    }

}
