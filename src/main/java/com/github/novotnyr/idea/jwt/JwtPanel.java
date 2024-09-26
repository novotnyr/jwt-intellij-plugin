package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.action.AbstractActionButtonController;
import com.github.novotnyr.idea.jwt.action.AddClaimActionButtonController;
import com.github.novotnyr.idea.jwt.action.EditClaimActionButtonUpdater;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.github.novotnyr.idea.jwt.ui.ClaimTableTransferHandler;
import com.github.novotnyr.idea.jwt.ui.IdePreferenceNewSignatureContextProvider;
import com.github.novotnyr.idea.jwt.ui.UiUtils;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.TextTransferable;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.EDIT;
import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.VIEW;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class JwtPanel implements DataProvider {
    @Nullable
    private Project project;

    private PropertyChangeSupport propertyChangeSupport;

    private JPanel rootPanel;

    private JLabel headerLabel;

    private JwtHeaderTableModel headerTableModel;

    private JBTable headerTable;

    private JLabel payloadLabel;

    private JwtClaimsTableModel claimsTableModel;

    private JBTable claimsTable;

    private JPanel claimsTablePanel;

    private JPanel secretPanelContainer;

    private SecretPanel secretPanel = new UnrecognizedSecretPanel();

    private JButton validateButton;

    private Jwt jwt = Jwt.EMPTY;

    private AddClaimActionButtonController addClaimActionButtonController;

    private EditClaimActionButtonUpdater editClaimActionButtonUpdater;

    private AbstractActionButtonController removeClaimActionButtonController;

    private boolean jwtUpdateInProgress;

    public JwtPanel() {
        this.headerLabel.setVisible(false);
        this.payloadLabel.setVisible(false);
        this.claimsTable.setName(Constants.CLAIMS_TABLE_NAME);

        initializeClaimsTableModel(this.claimsTable);
        configureClaimsTablePopup();
        configureClipboardCopy(this.claimsTable);

        replaceSecretPanelContent(this.secretPanel);
        configureSecretTextFieldListeners();

        this.validateButton.setEnabled(false);
        this.validateButton.addActionListener(this::onValidateButtonClick);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent mouseEvent) {
                return onClaimsTableDoubleClick(mouseEvent);
            }
        }.installOn(this.claimsTable);
    }

    private void createUIComponents() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.claimsTable = new JBTable();
        this.claimsTablePanel = configureClaimsTableActions();
    }

    public JPanel getRootPanel() {
        return this.rootPanel;
    }


    private void initializeClaimsTableModel(JBTable claimsTable) {
        this.claimsTableModel = new JwtClaimsTableModel(Jwt.EMPTY);
        claimsTable.setModel(this.claimsTableModel);
    }


    private void configureClipboardCopy(@SuppressWarnings("unused") JBTable claimsTable) {
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
                .addExtraAction(new AnActionButton("Copy payload as JSON", AllIcons.FileTypes.Json) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        onCopyAsJsonActionPerformed(anActionEvent);
                    }

                    @Override
                    public boolean isEnabled() {
                        return !JwtPanel.this.jwt.getPayloadClaims().isEmpty();
                    }
                })
                .setEditAction(this::onEditAction)
                .setEditActionUpdater(this.editClaimActionButtonUpdater)
                .setAddAction(this.addClaimActionButtonController)
                .setAddActionUpdater(this.addClaimActionButtonController)
                .setRemoveAction(this.removeClaimActionButtonController)
                .setRemoveActionUpdater(this.removeClaimActionButtonController)
                .createPanel();
        return this.claimsTablePanel;
    }


    private void configureClaimsTablePopup() {
        JPopupMenu popupMenu = new JPopupMenu();
        UiUtils.configureTableRowSelectionOnPopup(popupMenu);

        JMenuItem copyValueMenuItem = new JMenuItem("Copy value (as string)");
        copyValueMenuItem.addActionListener(JwtPanel.this::onCopyValueMenuItemActionPerformed);
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

    private void configureSecretTextFieldListeners() {
        this.secretPanel.setSignatureContextChangedListener(newSignatureContext -> refreshClaimsTableControllers());
    }

    private void editClaimAtRow(int row) {
        NamedClaim<?> claim = this.claimsTableModel.getClaimAt(row);
        showClaimDialog(claim, isEditable() ? EDIT : VIEW);
    }


    private void onCopyAsJsonActionPerformed(@SuppressWarnings("unused") AnActionEvent anActionEvent) {
        if (this.jwt == null || this.jwt.isEmpty()) {
            return;
        }
        TextTransferable textTransferable = new TextTransferable(JwtHelper.prettyUnbase64Json(this.jwt.getPayloadString()));
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }


    public void onCopyValueMenuItemActionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        int selectedRowIndex = this.claimsTable.getSelectedRow();
        Object claimValue = this.claimsTableModel.getValueAt(selectedRowIndex, 1);
        TextTransferable textTransferable = new TextTransferable(claimValue.toString());
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    public void onCopyKeyAndValueMenuItemActionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        int selectedRowIndex = this.claimsTable.getSelectedRow();
        Object claimName = this.claimsTableModel.getValueAt(selectedRowIndex, 0);
        Object claimValue = this.claimsTableModel.getValueAt(selectedRowIndex, 1);
        TextTransferable textTransferable = new TextTransferable(claimName + "=" + claimValue.toString());

        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    private void onValidateButtonClick(@SuppressWarnings("unused") ActionEvent event) {
        try {
            JwtValidator jwtValidator = new JwtValidator();

            SignatureContext secret = this.secretPanel.getSignatureContext();
            jwtValidator.validate(this.jwt, secret);
            this.claimsTableModel.setClaimErrors(jwtValidator.getClaimErrors());
            if(jwtValidator.hasSignatureError()) {
                this.secretPanel.notifySignatureErrors(jwtValidator.getSignatureError());
            } else {
                this.secretPanel.notifySignatureValid();
            }
        } catch (SignatureContextException e) {
            this.secretPanel.notifySecurityContextException(e);
        }
    }

    private boolean onClaimsTableDoubleClick(MouseEvent mouseEvent) {
        if(!isEditable()) {
            this.secretPanel.notifyEmptySignature();
        }

        int selectedRow = this.claimsTable.rowAtPoint(mouseEvent.getPoint());
        if(selectedRow < 0) {
            return true;
        }
        editClaimAtRow(selectedRow);

        return true;
    }

    private void onEditAction(@SuppressWarnings("unused") AnActionButton anActionButton) {
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
        this.jwt.setSignatureContext(getSignatureContext());
        this.jwt.removeClaim(claim.getName());

        refreshJwt();
    }


    private void showClaimDialog(NamedClaim<?> claim, ClaimDialog.Mode mode) {
        ClaimDialog claimDialog = new ClaimDialog(claim, mode);
        if(claimDialog.showAndGet()) {
            NamedClaim<?> updatedClaim = claimDialog.getClaim();
            this.jwt.setSignatureContext(getSignatureContext());
            this.jwt.setPayloadClaim(updatedClaim);
            refreshJwt();
        }
    }

    private void refreshClaimsTableControllers() {
        DataContext dataContext = dataId -> {
            if (Constants.DataKeys.JWT_STATUS.is(dataId)) {
                return JwtPanel.this.getSecretPanel().getStatus();
            }
            if (Constants.DataKeys.JWT.is(dataId)) {
                return JwtPanel.this.jwt;
            }
            return null;
        };

        AnActionEvent event = AnActionEvent.createFromInputEvent(null, "place", null, dataContext);
        this.addClaimActionButtonController.isEnabled(event);
        this.editClaimActionButtonUpdater.isEnabled(event);
        this.validateButton.setEnabled(isValidatable());
    }

    @Nullable
    @Override
    public Object getData(String dataId) {
        if (Constants.DataKeys.SECRET.is(dataId)) {
            return this.secretPanel.getSignatureContext();
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
        this.jwtUpdateInProgress = true;
        this.propertyChangeSupport.firePropertyChange("jwt", null, this.jwt);
        this.jwtUpdateInProgress = false;
    }

    public void setJwt(Jwt jwt) {
        if (this.jwtUpdateInProgress) {
            // this means that change in claims triggered
            // a change in String representation that
            // again triggered a change in claims
            // In such cacse, do nothing
            return;
        }

        this.jwt = jwt;
        this.headerTableModel = new JwtHeaderTableModel(jwt);
        this.headerTable.setModel(this.headerTableModel);

        if (Jwt.EMPTY.equals(jwt)) {
            this.headerLabel.setVisible(false);
            this.payloadLabel.setVisible(false);
            this.validateButton.setEnabled(false);
        } else {
            this.headerLabel.setVisible(true);
            this.payloadLabel.setVisible(true);
            this.validateButton.setEnabled(true);
        }

        this.claimsTableModel = new JwtClaimsTableModel(jwt);
        this.claimsTableModel.setClaimErrors(validateClaims(jwt));
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.setDefaultRenderer(Object.class, this.claimsTableModel);

        configureSecretPanel(jwt);

        refreshClaimsTableControllers();
    }

    private void replaceSecretPanelContent(SecretPanel secretPanel) {
        this.secretPanelContainer.removeAll();
        this.secretPanelContainer.add(secretPanel.getRoot(), BorderLayout.CENTER);
    }

    private void configureSecretPanel(Jwt jwt) {
        SecretPanel newSecretPanel = SecretPanelFactory.getInstance().getSecretPanel(project, jwt, getInitialSignatureContext(jwt));
        if (isSameSecretPanel(newSecretPanel)) {
            return;
        }
        newSecretPanel.setProject(this.project);
        this.secretPanel.removeSignatureContextChangedListener();
        replaceSecretPanelContent(newSecretPanel);
        this.secretPanel = newSecretPanel;
        configureSecretTextFieldListeners();
    }

    private SignatureContext getInitialSignatureContext(Jwt jwt) {
        if (this.project == null) {
            return SignatureContext.EMPTY;
        }
        return new IdePreferenceNewSignatureContextProvider(this.project)
                .createSignatureContext(jwt.getAlgorithm());
    }

    private boolean isSameSecretPanel(SecretPanel newSecretPanel) {
        return newSecretPanel.getClass().getName().equals(this.secretPanel.getClass().getName());
    }

    private List<ClaimError> validateClaims(Jwt jwt) {
        if (Jwt.EMPTY.equals(jwt)) {
            return Collections.emptyList();
        }
        return new JwtValidator().validateClaims(jwt).getClaimErrors();
    }


    public void setSignatureContext(SignatureContext signatureContext) {
        this.secretPanel.setSignatureContext(signatureContext);
    }

    private boolean isEditable() {
        return MUTABLE == this.secretPanel.getStatus();
    }

    private boolean isValidatable() {
        return this.secretPanel.getStatus().isValidatable();
    }


    public SecretPanel getSecretPanel() {
        return this.secretPanel;
    }

    private boolean isRemoveClaimActionEnabled() {
        return isEditable() && claimsTable.getSelectedRows().length > 0;
    }

    private SignatureContext getSignatureContext() {
        return this.getSecretPanel().getSignatureContext();
    }

    public void reset() {
        setJwt(Jwt.EMPTY);
    }

    public void addJwtListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener("jwt", listener);
    }

    public void setProject(@Nullable Project project) {
        this.project = project;
    }
}
