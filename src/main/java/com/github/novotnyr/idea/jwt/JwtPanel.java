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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.EDIT;
import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.VIEW;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class JwtPanel implements DataProvider, Disposable {
    private final Project project;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private JPanel rootPanel = new BorderLayoutPanel();

    private JwtHeaderTableModel headerTableModel;

    private JBTable headerTable = new JBTable();

    private JwtClaimsTableModel claimsTableModel;

    private JBTable claimsTable = new JBTable();

    private JPanel claimsTablePanel;

    private JBSplitter payloadSplitter;

    private JPanel secretPanelContainer = new JPanel(new BorderLayout());

    private final SecretPanel unrecognizedSecretPanel = new UnrecognizedSecretPanel();

    private final Map<String, SecretPanel> secretPanels = new HashMap<>();

    private SecretPanel secretPanel = this.unrecognizedSecretPanel;

    private JButton validateButton = new JButton("Validate");

    private Jwt jwt = Jwt.EMPTY;

    private AddClaimActionButtonController addClaimActionButtonController;

    private EditClaimActionButtonUpdater editClaimActionButtonUpdater;

    private AbstractActionButtonController removeClaimActionButtonController;

    private boolean jwtUpdateInProgress;

    public JwtPanel(Project project) {
        this.project = project;
        Disposer.register(this, this.unrecognizedSecretPanel);
        createUIComponents();

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
            protected boolean onDoubleClick(@NonNull MouseEvent mouseEvent) {
                return onClaimsTableDoubleClick(mouseEvent);
            }
        }.installOn(this.claimsTable);
    }

    private void createUIComponents() {
        this.claimsTablePanel = configureClaimsTableActions();
        updateClaimsTablePreferredSize();

        this.rootPanel.add(createHeaderTableAndClaimsLabelGroup(), BorderLayout.NORTH);
        this.rootPanel.add(createPayloadAndSecretPanels(), BorderLayout.CENTER);
        this.rootPanel.add(this.validateButton, BorderLayout.SOUTH);
    }

    private JComponent createHeaderTableAndClaimsLabelGroup() {
        this.headerTable.setAutoResizeMode(JBTable.AUTO_RESIZE_ALL_COLUMNS);
        this.headerTable.setTableHeader(null);
        this.headerTable.setPreferredScrollableViewportSize(new Dimension(0, 2 * this.headerTable.getRowHeight()));

        return this.headerTable;
    }

    private JComponent createPayloadAndSecretPanels() {
        this.payloadSplitter = new JBSplitter(true);
        this.payloadSplitter.setFirstComponent(this.claimsTablePanel);
        this.payloadSplitter.setSecondComponent(this.secretPanelContainer);
        return this.payloadSplitter;
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
            public boolean isEnabled(@NonNull AnActionEvent anActionEvent) {
                return isRemoveClaimActionEnabled();
            }
        };

        this.claimsTablePanel = ToolbarDecorator.createDecorator(this.claimsTable)
                .disableUpDownActions()
                .addExtraAction(new AnAction("Copy payload as JSON", null, AllIcons.FileTypes.Json) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                        onCopyAsJsonActionPerformed(anActionEvent);
                    }

                    @Override
                    public void update(@NotNull AnActionEvent e) {
                        e.getPresentation()
                         .setEnabled(!JwtPanel.this.jwt.getPayloadClaims().isEmpty());
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.BGT;
                    }
                })
                .setEditAction(this::onEditAction)
                .setEditActionUpdater(this.editClaimActionButtonUpdater)
                .setAddAction(this.addClaimActionButtonController)
                .setAddActionUpdater(this.addClaimActionButtonController)
                .setRemoveAction(this.removeClaimActionButtonController)
                .setRemoveActionUpdater(this.removeClaimActionButtonController)
                .createPanel();
        this.claimsTablePanel.setBorder(IdeBorderFactory.createTitledBorder("Payload", false));
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
    public Object getData(@NonNull String dataId) {
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
            // This means that a change in the claims triggered
            // a change in String representation that
            // again triggered a change in tjeclaims.
            // In such a case, do nothing.
            return;
        }

        this.jwt = jwt;
        this.headerTableModel = new JwtHeaderTableModel(jwt);
        this.headerTable.setModel(this.headerTableModel);
        this.headerTable.setPreferredScrollableViewportSize(new Dimension(0, this.headerTableModel.getRowCount() * this.headerTable.getRowHeight()));
        this.headerTable.getParent().revalidate();

        this.validateButton.setEnabled(!Jwt.EMPTY.equals(jwt));

        this.claimsTableModel = new JwtClaimsTableModel(jwt);
        this.claimsTableModel.setClaimErrors(validateClaims(jwt));
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.setDefaultRenderer(Object.class, this.claimsTableModel);
        this.claimsTableModel.addTableModelListener(e -> updateClaimsTablePreferredSize());
        updateClaimsTablePreferredSize();

        configureSecretPanel(jwt);

        refreshClaimsTableControllers();
    }

    private void updateClaimsTablePreferredSize() {
        int rowCount = this.claimsTableModel != null ? this.claimsTableModel.getRowCount() : 0;
        int rows = rowCount == 0 ? 3 : rowCount;
        this.claimsTable.setPreferredScrollableViewportSize(new Dimension(0, rows * this.claimsTable.getRowHeight()));
        this.claimsTable.getParent().revalidate();
        SwingUtilities.invokeLater(() -> {
            if (this.payloadSplitter != null && this.payloadSplitter.getHeight() > 0) {
                int prefHeight = this.claimsTablePanel.getPreferredSize().height;
                this.payloadSplitter.setProportion((float) prefHeight / this.payloadSplitter.getHeight());
            }
        });
    }

    private void replaceSecretPanelContent(SecretPanel secretPanel) {
        this.secretPanelContainer.removeAll();
        this.secretPanelContainer.add(secretPanel.getRoot(), BorderLayout.CENTER);
        this.secretPanelContainer.revalidate();
        this.secretPanelContainer.repaint();
    }

    private void configureSecretPanel(Jwt jwt) {
        SecretPanel newSecretPanel = getOrCreateSecretPanel(jwt.getAlgorithm());
        if (newSecretPanel == this.secretPanel) {
            return;
        }
        this.secretPanel.removeSignatureContextChangedListener();
        replaceSecretPanelContent(newSecretPanel);
        this.secretPanel = newSecretPanel;
        configureSecretTextFieldListeners();
    }

    private SecretPanel getOrCreateSecretPanel(String algorithm) {
        if (!SignatureAlgorithm.isSupported(algorithm)) {
            return this.unrecognizedSecretPanel;
        }

        SecretPanel panel = this.secretPanels.get(algorithm);
        if (panel == null) {
            SignatureContext initialSignatureContext = getInitialSignatureContext(algorithm);
            panel = SecretPanelFactory.create(this.project, algorithm, initialSignatureContext);
            Disposer.register(this, panel);
            this.secretPanels.put(algorithm, panel);
        }
        return panel;
    }

    private SignatureContext getInitialSignatureContext(String algorithm) {
        return new IdePreferenceNewSignatureContextProvider(this.project)
                .createSignatureContext(algorithm);
    }

    @Nullable
    public SecretPanel copySecretPanelForDialog(String algorithm) {
        SecretPanel panel = this.secretPanels.get(algorithm);
        return panel == null ? null : panel.copyFor(this.project);
    }

    private List<ClaimError> validateClaims(Jwt jwt) {
        if (Jwt.EMPTY.equals(jwt)) {
            return Collections.emptyList();
        }
        return new JwtValidator().validateClaims(jwt).getClaimErrors();
    }


    public void setSignatureContext(SignatureContext signatureContext) {
        this.secretPanel.removeSignatureContextChangedListener();
        try {
            this.secretPanel.setSignatureContext(signatureContext);
        } finally {
            configureSecretTextFieldListeners();
        }
        refreshClaimsTableControllers();
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
        return isEditable() && this.claimsTable.getSelectedRows().length > 0;
    }

    private SignatureContext getSignatureContext() {
        return this.getSecretPanel().getSignatureContext();
    }

    public void reset() {
        setJwt(Jwt.EMPTY);
        for (SecretPanel panel : this.secretPanels.values()) {
            Disposer.dispose(panel);
        }
        this.secretPanels.clear();
    }

    public void addJwtListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener("jwt", listener);
    }

    @Override
    public void dispose() {
        this.secretPanel.removeSignatureContextChangedListener();
        this.secretPanels.clear();
    }
}
