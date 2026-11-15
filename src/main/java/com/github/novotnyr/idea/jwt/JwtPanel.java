package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.action.AddClaimActionButtonController;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RsaKeyParser;
import com.github.novotnyr.idea.jwt.ui.ClaimTableTransferHandler;
import com.github.novotnyr.idea.jwt.ui.IdePreferenceNewSignatureContextProvider;
import com.github.novotnyr.idea.jwt.ui.UiUtils;
import com.github.novotnyr.idea.jwt.ui.preferences.PluginPreferences;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
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
import com.intellij.util.concurrency.AppExecutorUtil;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.EDIT;
import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.VIEW;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class JwtPanel implements DataProvider, Disposable {
    private static final Logger LOG = Logger.getInstance(JwtPanel.class);

    private final Project project;

    /**
     * Loads configured RSA key files and parses their PEM content off the EDT.
     * The completion handler applies the result on the EDT only if the panel still owns the captured key text.
     */
    private final Executor rsaExecutor;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private final JPanel rootPanel = new BorderLayoutPanel();

    private JwtHeaderTableModel headerTableModel;

    private final JBTable headerTable = new JBTable();

    private JwtClaimsTableModel claimsTableModel;

    private final JBTable claimsTable = new JBTable();

    private JPanel claimsTablePanel;

    private JBSplitter payloadSplitter;

    private final JPanel secretPanelContainer = new JPanel(new BorderLayout());

    private final SecretPanel unrecognizedSecretPanel = new UnrecognizedSecretPanel();

    private final Map<String, SecretPanel> secretPanels = new HashMap<>();

    private SecretPanel secretPanel = this.unrecognizedSecretPanel;

    private final JButton validateButton = new JButton("Validate");

    private Jwt jwt = Jwt.EMPTY;

    private boolean jwtUpdateInProgress;

    /** Prevents delayed RSA work from updating this panel after disposal. */
    private boolean disposed;

    /**
     * Key text last populated from preferences, including its document stamps.
     * A settings change reloads defaults only while the current text still matches this snapshot.
     */
    private RS256Panel.KeyTextSnapshot rsaDefaultsText;

    /** Invalidates older default-key loads after a newer load or Reset. */
    private long rsaDefaultsGeneration;

    /**
     * Last successfully parsed RSA context. Cleared when the key text changes; volatile because
     * action-system data lookups may read it on another thread without parsing editor text.
     */
    private volatile RS256SignatureContext parsedRsaContext;

    /** JWT and parsed keys produced together by an RSA claim update on a worker thread. */
    private record RsaMutation(Jwt jwt, SignatureContext context) {
    }

    /** Signature-validation result and parsed keys produced together on a worker thread. */
    private record RsaValidation(JwtValidator validator, SignatureContext context) {
    }

    public JwtPanel(Project project) {
        this(project, AppExecutorUtil.getAppExecutorService());
    }

    JwtPanel(Project project, Executor rsaExecutor) {
        this.project = project;
        this.rsaExecutor = rsaExecutor;
        Disposer.register(this, this.unrecognizedSecretPanel);
        PluginPreferences.getInstance(project).addKeyFilesChangeListener(this::onKeyFilesChanged, this);
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
        AddClaimActionButtonController addClaimAction = new AddClaimActionButtonController(this::isAddClaimActionEnabled) {
            @Override
            public void onClaimTypeSelected(DataTypeRegistry.DataType dataType) {
                onNewAction(dataType);
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
                        return ActionUpdateThread.EDT;
                    }
                })
                .setEditAction(this::onEditAction)
                .setEditActionUpdater(event -> isEditClaimActionEnabled())
                .setAddAction(addClaimAction)
                .setAddActionUpdater(addClaimAction)
                .setRemoveAction(button -> onRemoveClaim())
                .setRemoveActionUpdater(event -> isRemoveClaimActionEnabled())
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
        this.secretPanel.setSignatureContextChangedListener(() -> {
            if (this.secretPanel instanceof RS256Panel) {
                this.parsedRsaContext = null;
            }
            refreshValidateButton();
        });
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
        RS256Panel rsaPanel = this.secretPanel instanceof RS256Panel panel ? panel : null;
        RS256Panel.KeyTextSnapshot keyText = rsaPanel == null ? null : rsaPanel.snapshotKeyText();
        Jwt originalJwt = this.jwt;
        String encodedJwt = originalJwt.toString();
        try {
            JwtValidator jwtValidator;
            if (rsaPanel != null) {
                RsaValidation validation = runRsaOperation("Validating RSA JWT", () -> {
                    SignatureContext context = RsaKeyParser.parse(keyText.publicKey(), keyText.privateKey());
                    JwtValidator validator = new JwtValidator();
                    validator.validate(new Jwt(encodedJwt), context);
                    return new RsaValidation(validator, context);
                });
                if (validation == null || !isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                    return;
                }
                jwtValidator = validation.validator();
                this.parsedRsaContext = (RS256SignatureContext) validation.context();
            } else {
                jwtValidator = new JwtValidator();
                jwtValidator.validate(this.jwt, this.secretPanel.getSignatureContext());
            }
            this.claimsTableModel.setClaimErrors(jwtValidator.getClaimErrors());
            if(jwtValidator.hasSignatureError()) {
                this.secretPanel.notifySignatureErrors(jwtValidator.getSignatureError());
            } else {
                this.secretPanel.notifySignatureValid();
            }
        } catch (SignatureContextException e) {
            if (rsaPanel == null || isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                this.secretPanel.notifySecurityContextException(e);
            }
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
        if (!isRemoveClaimActionEnabled()) {
            return;
        }
        int selectedRow = this.claimsTable.getSelectedRow();
        NamedClaim<?> claim = this.claimsTableModel.getClaimAt(selectedRow);
        updateClaim(null, claim.getName());
    }


    private void showClaimDialog(NamedClaim<?> claim, ClaimDialog.Mode mode) {
        ClaimDialog claimDialog = new ClaimDialog(claim, mode);
        if(claimDialog.showAndGet()) {
            NamedClaim<?> updatedClaim = claimDialog.getClaim();
            updateClaim(updatedClaim, null);
        }
    }

    void updateClaim(@Nullable NamedClaim<?> updatedClaim, @Nullable String removedClaimName) {
        if (this.secretPanel instanceof RS256Panel rsaPanel) {
            RS256Panel.KeyTextSnapshot keyText = rsaPanel.snapshotKeyText();
            Jwt originalJwt = this.jwt;
            String encodedJwt = originalJwt.toString();
            try {
                RsaMutation mutation = runRsaOperation("Updating RSA JWT", () -> {
                    SignatureContext context = RsaKeyParser.parse(keyText.publicKey(), keyText.privateKey());
                    Jwt updatedJwt = new Jwt(encodedJwt);
                    updatedJwt.setSignatureContext(context);
                    if (updatedClaim != null) {
                        updatedJwt.setPayloadClaim(updatedClaim);
                    } else {
                        updatedJwt.removeClaim(removedClaimName);
                    }
                    return new RsaMutation(updatedJwt, context);
                });
                if (mutation == null || !isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                    return;
                }
                this.jwt = mutation.jwt();
                this.parsedRsaContext = (RS256SignatureContext) mutation.context();
                refreshJwt();
            } catch (SignatureContextException e) {
                if (isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                    rsaPanel.notifySecurityContextException(e);
                }
            }
            return;
        }
        this.jwt.setSignatureContext(getSignatureContext());
        if (updatedClaim != null) {
            this.jwt.setPayloadClaim(updatedClaim);
        } else {
            this.jwt.removeClaim(removedClaimName);
        }
        refreshJwt();
    }

    private boolean isCurrentRsaInput(RS256Panel panel, RS256Panel.KeyTextSnapshot keyText,
                                      Jwt originalJwt, String encodedJwt) {
        return !this.disposed && !this.project.isDisposed() && this.secretPanel == panel
                && panel.snapshotKeyText().equals(keyText) && this.jwt == originalJwt
                && encodedJwt.equals(this.jwt.toString());
    }

    @Nullable
    private <T> T runRsaOperation(String title, Supplier<T> operation) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                result.set(operation.get());
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (RuntimeException e) {
                failure.set(e);
            }
        }, title, true, this.project);
        if (!completed) {
            return null;
        }
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private void refreshValidateButton() {
        this.validateButton.setEnabled(!Jwt.EMPTY.equals(this.jwt) && isValidatable());
    }

    @Nullable
    @Override
    public Object getData(@NonNull String dataId) {
        if (Constants.DataKeys.SECRET.is(dataId)) {
            if (this.secretPanel instanceof RS256Panel) {
                // DataProvider may be queried during action updates; never parse editor text here.
                return this.parsedRsaContext;
            }
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

        this.claimsTableModel = new JwtClaimsTableModel(jwt);
        this.claimsTableModel.setClaimErrors(validateClaims(jwt));
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.setDefaultRenderer(Object.class, this.claimsTableModel);
        this.claimsTableModel.addTableModelListener(e -> updateClaimsTablePreferredSize());
        updateClaimsTablePreferredSize();

        configureSecretPanel(jwt);

        refreshValidateButton();
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
            panel = SecretPanelFactory.create(this.project, algorithm, SignatureContext.EMPTY);
            Disposer.register(this, panel);
            this.secretPanels.put(algorithm, panel);
            if (panel instanceof RS256Panel rs256Panel) {
                this.rsaDefaultsText = rs256Panel.snapshotKeyText();
                loadDefaultRsaKeys(rs256Panel);
            }
        }
        return panel;
    }

    private void loadDefaultRsaKeys(RS256Panel panel) {
        IdePreferenceNewSignatureContextProvider provider = new IdePreferenceNewSignatureContextProvider(this.project);
        RS256Panel.KeyTextSnapshot initialText = panel.snapshotKeyText();
        long generation = ++this.rsaDefaultsGeneration;
        CompletableFuture.supplyAsync(() -> provider.<SignatureContext>createSignatureContext(SignatureAlgorithm.RS256),
                        this.rsaExecutor)
                .whenComplete((context, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        LOG.warn("Cannot load default RSA keys", error);
                        return;
                    }
                    if (this.disposed || this.project.isDisposed() || generation != this.rsaDefaultsGeneration
                            || this.secretPanels.get(SignatureAlgorithm.RS256) != panel
                            || !panel.snapshotKeyText().equals(initialText)) {
                        return;
                    }
                    if (context instanceof RS256SignatureContext) {
                        if (this.secretPanel == panel) {
                            setSignatureContext(context);
                        } else {
                            panel.setSignatureContext(context);
                            this.parsedRsaContext = (RS256SignatureContext) context;
                        }
                        this.rsaDefaultsText = panel.snapshotKeyText();
                    }
                }));
    }

    private void onKeyFilesChanged() {
        SecretPanel panel = this.secretPanels.get(SignatureAlgorithm.RS256);
        if (panel instanceof RS256Panel rsaPanel
                && rsaPanel.snapshotKeyText().equals(this.rsaDefaultsText)) {
            loadDefaultRsaKeys(rsaPanel);
        }
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
            if (this.secretPanel instanceof RS256Panel) {
                this.parsedRsaContext = (RS256SignatureContext) signatureContext;
            }
        } finally {
            configureSecretTextFieldListeners();
        }
        refreshValidateButton();
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

    private boolean isAddClaimActionEnabled() {
        return !Jwt.EMPTY.equals(this.jwt) && isEditable();
    }

    private boolean isEditClaimActionEnabled() {
        return isEditable() && this.claimsTable.getSelectedRow() >= 0;
    }

    private boolean isRemoveClaimActionEnabled() {
        return isEditClaimActionEnabled();
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
        this.rsaDefaultsText = null;
        this.rsaDefaultsGeneration++;
        this.parsedRsaContext = null;
    }

    public void addJwtListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener("jwt", listener);
    }

    @Override
    public void dispose() {
        this.disposed = true;
        this.secretPanel.removeSignatureContextChangedListener();
        this.secretPanels.clear();
        this.parsedRsaContext = null;
    }
}
