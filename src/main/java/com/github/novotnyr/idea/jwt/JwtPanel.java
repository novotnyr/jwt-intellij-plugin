package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.novotnyr.idea.jwt.Configuration.TimestampFormat;
import com.github.novotnyr.idea.jwt.action.AddClaimActionButtonController;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs384.HS384Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RsaKeyParser;
import com.github.novotnyr.idea.jwt.ui.ClaimTableTransferHandler;
import com.github.novotnyr.idea.jwt.ui.IdePreferenceNewSignatureContextProvider;
import com.github.novotnyr.idea.jwt.ui.preferences.PluginPreferences;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtClaimsValidator;
import com.github.novotnyr.idea.jwt.validation.JwtSignatureValidator;
import com.github.novotnyr.idea.jwt.validation.SignatureError;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.EDIT;
import static com.github.novotnyr.idea.jwt.ClaimDialog.Mode.VIEW;
import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class JwtPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(JwtPanel.class);

    private final Project project;

    /**
     * Loads configured RSA key files and parses their PEM content off the EDT.
     * The completion handler applies the result on the EDT only if the panel still owns the captured key text.
     */
    private final Executor rsaExecutor;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private final JPanel rootPanel = new BorderLayoutPanel();

    private final JBTabbedPane parsedTabs = new JBTabbedPane();

    private final JPanel expirationWarningPanel = new JPanel(new BorderLayout(JBUI.scale(8), 0));

    private final JLabel expirationWarningMessage = new JLabel();

    private final JPanel feedbackPanel = new JPanel();

    private final JPanel signatureResultPanel = new JPanel(new BorderLayout(JBUI.scale(8), 0));

    private final JLabel signatureResultIcon = new JLabel();

    private final JLabel signatureResultMessage = new JLabel();

    private final JLabel missingVerificationInputMessage = new JLabel();

    private final Color signatureResultWarningBackground = JBColor.namedColor("Notification.warningBackground",
            new JBColor(0xFFF4D1, 0x4B422A));

    private final Color signatureResultWarningForeground = JBColor.namedColor("Notification.warningForeground",
            new JBColor(0x594300, 0xF0D98C));

    private JwtHeaderTableModel headerTableModel;

    private final JBTable headerTable = new ClaimValueTable();

    private JwtClaimsTableModel claimsTableModel;

    private final JBTable claimsTable = new ClaimValueTable();

    private JBPopup claimErrorPopup;

    private int claimErrorPopupModelRow = -1;

    private String claimErrorPopupMessage;

    private boolean claimErrorPopupTriggeredByMouse;

    private JPanel claimsTablePanel;

    private final JPanel secretPanelContainer = new JPanel(new BorderLayout());

    private final UnrecognizedSecretPanel unrecognizedSecretPanel = new UnrecognizedSecretPanel();

    private final Map<String, SecretPanel> secretPanels = new HashMap<>();

    private SecretPanel secretPanel = this.unrecognizedSecretPanel;

    private final JButton verifySignatureButton = new JButton("Verify Signature");

    private final JPanel verificationToolbarComponent = new JPanel(new BorderLayout(JBUI.scale(8), 0));

    private final Clock clock;

    private final ClaimRefreshScheduler claimRefreshScheduler;

    private final JwtSignatureValidator signatureValidator = new JwtSignatureValidator();

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

    public JwtPanel(Project project) {
        this(project, AppExecutorUtil.getAppExecutorService(), Clock.systemUTC(), new SwingClaimRefreshScheduler());
    }

    JwtPanel(Project project, Executor rsaExecutor) {
        this(project, rsaExecutor, Clock.systemUTC(), new SwingClaimRefreshScheduler());
    }

    JwtPanel(Project project, Clock clock, ClaimRefreshScheduler claimRefreshScheduler) {
        this(project, AppExecutorUtil.getAppExecutorService(), clock, claimRefreshScheduler);
    }

    private JwtPanel(Project project, Executor rsaExecutor, Clock clock,
                     ClaimRefreshScheduler claimRefreshScheduler) {
        this.project = project;
        this.rsaExecutor = rsaExecutor;
        this.clock = clock;
        this.claimRefreshScheduler = claimRefreshScheduler;
        Disposer.register(this, this.unrecognizedSecretPanel);
        PluginPreferences.getInstance(project).addKeyFilesChangeListener(this::onKeyFilesChanged, this);
        configureExpirationWarningPanel();
        configureSignatureResultPanel();
        createUIComponents();

        this.claimsTable.setName(Constants.CLAIMS_TABLE_NAME);

        initializeClaimsTableModel(this.claimsTable);
        configureClaimErrorTooltips();
        configureClaimsTablePopup();
        configureHeaderTablePopup();
        configureClipboardCopy(this.claimsTable);

        replaceSecretPanelContent(this.secretPanel);
        configureSecretTextFieldListeners();

        this.verifySignatureButton.setName("jwt.verifySignature.button");
        this.verifySignatureButton.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, Boolean.TRUE);
        this.verifySignatureButton.setIcon(IconLoader.getIcon("/icons/checkConstraint.svg", JwtPanel.class));
        this.verifySignatureButton.setMinimumSize(this.verifySignatureButton.getPreferredSize());
        this.verifySignatureButton.setEnabled(false);
        this.verifySignatureButton.addActionListener(this::onVerifySignatureButtonClick);
        this.verificationToolbarComponent.setName("jwt.verification.controls");
        this.verificationToolbarComponent.setOpaque(false);
        this.verificationToolbarComponent.add(this.verifySignatureButton, BorderLayout.EAST);
        refreshVerifySignatureButton();

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NonNull MouseEvent mouseEvent) {
                return onClaimsTableDoubleClick(mouseEvent);
            }
        }.installOn(this.claimsTable);

        Configuration.INSTANCE.addTimestampFormatChangeListener(this::onTimestampFormatChanged, this);
    }

    private void createUIComponents() {
        this.claimsTablePanel = configureClaimsTableActions();
        updateClaimsTablePreferredSize();

        this.rootPanel.add(createTabbedContentAndSecretPanels(), BorderLayout.CENTER);
    }

    private JComponent createTabbedContentAndSecretPanels() {
        this.headerTableModel = new JwtHeaderTableModel(Jwt.EMPTY);
        this.headerTable.setModel(this.headerTableModel);
        configureClaimValueColumns(this.headerTable);
        this.headerTable.getEmptyText().setText("No valid JWT to display");
        this.claimsTable.getEmptyText().setText("No valid JWT to display");

        JBScrollPane headerScrollPane = new JBScrollPane(this.headerTable);
        headerScrollPane.setBorder(JBUI.Borders.empty());

        this.parsedTabs.addTab("Payload", this.claimsTablePanel);
        this.parsedTabs.addTab("Header", headerScrollPane);
        this.parsedTabs.addTab("Key / Secret", this.secretPanelContainer);

        JPanel parsedContent = new JPanel(new BorderLayout());
        parsedContent.add(this.feedbackPanel, BorderLayout.NORTH);
        parsedContent.add(this.parsedTabs, BorderLayout.CENTER);
        return parsedContent;
    }

    private void configureExpirationWarningPanel() {
        Color warningBackground = JBColor.namedColor("Notification.warningBackground",
                new JBColor(0xFFF4D1, 0x4B422A));
        this.expirationWarningPanel.setName("jwt.expiration.warning");
        this.expirationWarningPanel.setOpaque(true);
        this.expirationWarningPanel.setBackground(warningBackground);
        this.expirationWarningPanel.setBorder(JBUI.Borders.empty(8, 12));
        this.expirationWarningPanel.add(new JLabel(AllIcons.General.BalloonWarning), BorderLayout.WEST);
        this.expirationWarningMessage.setName("jwt.expiration.message");
        this.expirationWarningMessage.setForeground(JBColor.namedColor("Notification.warningForeground",
                new JBColor(0x594300, 0xF0D98C)));
        this.expirationWarningPanel.add(this.expirationWarningMessage, BorderLayout.CENTER);
        this.expirationWarningPanel.setVisible(false);
    }

    private void configureSignatureResultPanel() {
        this.feedbackPanel.setName("jwt.feedback.stack");
        this.feedbackPanel.setLayout(new BoxLayout(this.feedbackPanel, BoxLayout.Y_AXIS));
        this.feedbackPanel.setOpaque(false);
        this.feedbackPanel.add(this.expirationWarningPanel);

        this.signatureResultPanel.setName("jwt.signature.result");
        this.signatureResultPanel.setOpaque(true);
        this.signatureResultPanel.setBackground(this.signatureResultWarningBackground);
        this.signatureResultPanel.setBorder(JBUI.Borders.empty(8, 12));
        this.signatureResultIcon.setName("jwt.signature.icon");
        this.signatureResultIcon.setIcon(AllIcons.General.BalloonWarning);
        this.signatureResultPanel.add(this.signatureResultIcon, BorderLayout.WEST);
        this.signatureResultMessage.setName("jwt.signature.message");
        this.signatureResultMessage.setForeground(this.signatureResultWarningForeground);
        this.signatureResultPanel.add(this.signatureResultMessage, BorderLayout.CENTER);
        this.signatureResultPanel.setVisible(false);
        this.feedbackPanel.add(this.signatureResultPanel);
        this.missingVerificationInputMessage.setName("jwt.verification.input.hint");
        this.missingVerificationInputMessage.setBorder(JBUI.Borders.empty(4, 12, 8, 12));
        this.missingVerificationInputMessage.setVisible(false);
        this.feedbackPanel.add(this.missingVerificationInputMessage);
        this.feedbackPanel.setVisible(false);
    }

    public JPanel getRootPanel() {
        return this.rootPanel;
    }


    private void initializeClaimsTableModel(JBTable claimsTable) {
        this.claimsTableModel = new JwtClaimsTableModel(Jwt.EMPTY);
        claimsTable.setModel(this.claimsTableModel);
        configureClaimValueColumns(claimsTable);
        claimsTable.getColumnModel().getColumn(1).setCellRenderer(new ClaimErrorValueRenderer());
    }

    private void configureClaimErrorTooltips() {
        this.claimsTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                showClaimErrorTooltipForMousePoint(event.getPoint());
            }
        });
        this.claimsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                if (JwtPanel.this.claimErrorPopupTriggeredByMouse) {
                    dismissClaimErrorTooltip();
                }
            }
        });
        this.claimsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting() && JwtPanel.this.claimsTable.isFocusOwner()) {
                    showClaimErrorTooltipForFocusedRow();
                }
            }
        });
        this.claimsTable.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                showClaimErrorTooltipForFocusedRow();
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (!JwtPanel.this.claimErrorPopupTriggeredByMouse) {
                    dismissClaimErrorTooltip();
                }
            }
        });
        this.claimsTable.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && !JwtPanel.this.claimsTable.isShowing()) {
                dismissClaimErrorTooltip();
            }
        });
    }

    @Nullable
    ClaimError getClaimErrorAtViewCell(int viewRow, int viewColumn) {
        if (viewRow < 0 || viewRow >= this.claimsTable.getRowCount()
                || viewColumn < 0 || viewColumn >= this.claimsTable.getColumnCount()
                || this.claimsTable.convertColumnIndexToModel(viewColumn) != 1) {
            return null;
        }
        if (!(this.claimsTable.getModel() instanceof JwtClaimsTableModel model)) {
            return null;
        }
        return model.getClaimErrorAt(this.claimsTable.convertRowIndexToModel(viewRow));
    }

    @Nullable
    ClaimError getClaimErrorAtPoint(Point point) {
        return getClaimErrorAtViewCell(this.claimsTable.rowAtPoint(point),
                this.claimsTable.columnAtPoint(point));
    }

    @Nullable
    ClaimError getClaimErrorAtFocusedRow() {
        return getClaimErrorAtViewCell(getFocusedViewRow(), this.claimsTable.convertColumnIndexToView(1));
    }

    private int getFocusedViewRow() {
        int leadSelectionIndex = this.claimsTable.getSelectionModel().getLeadSelectionIndex();
        if (leadSelectionIndex < 0 || leadSelectionIndex >= this.claimsTable.getRowCount()
                || !this.claimsTable.isRowSelected(leadSelectionIndex)) {
            return -1;
        }
        return leadSelectionIndex;
    }

    private void showClaimErrorTooltipForMousePoint(Point point) {
        int viewRow = this.claimsTable.rowAtPoint(point);
        int viewColumn = this.claimsTable.columnAtPoint(point);
        showClaimErrorTooltipAtCell(viewRow, viewColumn, getClaimErrorAtPoint(point), true);
    }

    private void showClaimErrorTooltipAtCell(int viewRow, int viewColumn, @Nullable ClaimError claimError,
                                             boolean triggeredByMouse) {
        if (claimError == null || !this.claimsTable.isShowing()) {
            dismissClaimErrorTooltip();
            return;
        }

        int modelRow = this.claimsTable.convertRowIndexToModel(viewRow);
        String message = claimError.getMessage();
        if (this.claimErrorPopup != null && this.claimErrorPopup.isVisible()
                && this.claimErrorPopupModelRow == modelRow
                && message.equals(this.claimErrorPopupMessage)) {
            this.claimErrorPopupTriggeredByMouse = triggeredByMouse;
            return;
        }

        dismissClaimErrorTooltip();
        ValidationInfo validationInfo = new ValidationInfo(
                StringUtil.escapeXmlEntities(message), this.claimsTable);
        this.claimErrorPopup = ComponentValidator.createPopupBuilder(validationInfo, null)
                .setRequestFocus(false)
                .setFocusable(false)
                .createPopup();
        this.claimErrorPopupModelRow = modelRow;
        this.claimErrorPopupMessage = message;
        this.claimErrorPopupTriggeredByMouse = triggeredByMouse;

        Rectangle cellBounds = this.claimsTable.getCellRect(viewRow, viewColumn, true);
        this.claimErrorPopup.show(new RelativePoint(this.claimsTable,
                new Point(cellBounds.x, cellBounds.y + cellBounds.height)));
    }

    private void showClaimErrorTooltipForFocusedRow() {
        int viewRow = getFocusedViewRow();
        int viewColumn = this.claimsTable.convertColumnIndexToView(1);
        ClaimError claimError = getClaimErrorAtViewCell(viewRow, viewColumn);
        showClaimErrorTooltipAtCell(viewRow, viewColumn, claimError, false);
    }

    private void dismissClaimErrorTooltip() {
        JBPopup popup = this.claimErrorPopup;
        this.claimErrorPopup = null;
        this.claimErrorPopupModelRow = -1;
        this.claimErrorPopupMessage = null;
        this.claimErrorPopupTriggeredByMouse = false;
        if (popup != null) {
            popup.cancel();
        }
    }

    private static void configureClaimValueColumns(JBTable table) {
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateColumnsFromModel(false);
    }


    private void configureClipboardCopy(@SuppressWarnings("unused") JBTable claimsTable) {
        this.claimsTable.setTransferHandler(new ClaimTableTransferHandler());
    }

    private JPanel configureClaimsTableActions() {
        AddClaimActionButtonController addClaimAction = new AddClaimAction();

        this.claimsTablePanel = ToolbarDecorator.createDecorator(this.claimsTable)
                .disableUpDownActions()
                .addExtraAction(new CopyPayloadAsJsonAction())
                .setEditAction(this::onEditAction)
                .setEditActionUpdater(event -> isEditClaimActionEnabled())
                .setAddAction(addClaimAction)
                .setAddActionUpdater(addClaimAction)
                .setRemoveAction(button -> onRemoveClaim())
                .setRemoveActionUpdater(event -> isRemoveClaimActionEnabled())
                .createPanel();
        return this.claimsTablePanel;
    }


    private void configureClaimsTablePopup() {
        DefaultActionGroup actions = new DefaultActionGroup();
        actions.add(new CopyClaimAction("Copy value (as string)", this::copySelectedClaimValue));
        actions.add(new CopyClaimAction("Copy value (as key=value)", this::copySelectedClaimAsKeyAndValue));

        ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP, actions);
        popup.setTargetComponent(this.claimsTable);
        this.claimsTable.setComponentPopupMenu(popup.getComponent());
    }

    private void configureHeaderTablePopup() {
        DefaultActionGroup actions = new DefaultActionGroup();
        actions.add(new CopyHeaderValueAction());

        ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP, actions);
        popup.setTargetComponent(this.headerTable);
        this.headerTable.setComponentPopupMenu(popup.getComponent());
    }

    private void configureSecretTextFieldListeners() {
        this.secretPanel.setSignatureContextChangedListener(this::onSignatureInputChanged);
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


    private void copySelectedClaimValue() {
        int selectedRowIndex = this.claimsTable.getSelectedRow();
        Object claimValue = this.claimsTableModel.getValueAt(selectedRowIndex, 1);
        TextTransferable textTransferable = new TextTransferable(claimValue.toString());
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    private void copySelectedClaimAsKeyAndValue() {
        int selectedRowIndex = this.claimsTable.getSelectedRow();
        Object claimName = this.claimsTableModel.getValueAt(selectedRowIndex, 0);
        Object claimValue = this.claimsTableModel.getValueAt(selectedRowIndex, 1);
        TextTransferable textTransferable = new TextTransferable(claimName + "=" + claimValue.toString());

        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    void copySelectedHeaderValue() {
        int row = this.headerTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String value = (String) this.headerTable.getValueAt(row, 1);
        CopyPasteManagerEx.getInstanceEx().setContents(new TextTransferable(value));
    }

    private void onVerifySignatureButtonClick(@SuppressWarnings("unused") ActionEvent event) {
        RS256Panel rsaPanel = this.secretPanel instanceof RS256Panel panel ? panel : null;
        RS256Panel.KeyTextSnapshot keyText = rsaPanel == null ? null : rsaPanel.snapshotKeyText();
        Jwt originalJwt = this.jwt;
        String encodedJwt = originalJwt.toString();
        try {
            Optional<SignatureError> signatureError;
            if (rsaPanel != null) {
                DecodedJWT decodedJwt = originalJwt.getDecodedJwt();
                signatureError = runRsaOperation("Verifying RSA JWT", () -> {
                    SignatureContext context = RsaKeyParser.parse(keyText.publicKey(), keyText.privateKey());
                    return this.signatureValidator.validate(decodedJwt, context);
                });
                if (signatureError == null || !isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                    return;
                }
            } else {
                signatureError = this.signatureValidator.validate(originalJwt.getDecodedJwt(),
                        this.secretPanel.getSignatureContext());
            }
            if (signatureError.isPresent()) {
                showVerificationResult("Signature invalid: " + signatureError.get().message(), true);
            } else {
                showVerificationResult("Signature verified", false);
            }
        } catch (SignatureContextException e) {
            if (rsaPanel == null || isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                showVerificationResult("Signature verification failed: " + e.getMessage(), true);
            }
        }
    }

    private boolean onClaimsTableDoubleClick(MouseEvent mouseEvent) {
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
        String viewOnlyReason = mode == VIEW ? getClaimViewOnlyReason() : null;
        ClaimDialog claimDialog = new ClaimDialog(claim, mode, viewOnlyReason);
        if (claimDialog.showAndGet() && mode != VIEW) {
            NamedClaim<?> updatedClaim = claimDialog.getClaim();
            updateClaim(updatedClaim, null);
        }
    }

    String getClaimViewOnlyReason() {
        if (this.secretPanel instanceof HS256Panel || this.secretPanel instanceof HS384Panel) {
            return "Read-only: Enter a shared secret in Key / Secret to edit claims.";
        }
        if (this.secretPanel instanceof RS256Panel) {
            return "Read-only: Enter the RSA public and private keys in Key / Secret to edit claims.";
        }
        return "Read-only: The token's algorithm is not supported.";
    }

    void updateClaim(@Nullable NamedClaim<?> updatedClaim, @Nullable String removedClaimName) {
        if (this.secretPanel instanceof RS256Panel rsaPanel) {
            RS256Panel.KeyTextSnapshot keyText = rsaPanel.snapshotKeyText();
            Jwt originalJwt = this.jwt;
            String encodedJwt = originalJwt.toString();
            try {
                Jwt updatedJwt = runRsaOperation("Updating RSA JWT", () -> {
                    SignatureContext context = RsaKeyParser.parse(keyText.publicKey(), keyText.privateKey());
                    Jwt jwtToUpdate = new Jwt(encodedJwt);
                    jwtToUpdate.setSignatureContext(context);
                    if (updatedClaim != null) {
                        jwtToUpdate.setPayloadClaim(updatedClaim);
                    } else {
                        jwtToUpdate.removeClaim(removedClaimName);
                    }
                    return jwtToUpdate;
                });
                if (updatedJwt == null || !isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                    return;
                }
                this.jwt = updatedJwt;
                refreshJwt();
            } catch (SignatureContextException e) {
                if (isCurrentRsaInput(rsaPanel, keyText, originalJwt, encodedJwt)) {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("JWT signing failed: " + e.getMessage(),
                                    MessageType.ERROR, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(this.parsedTabs), Balloon.Position.atRight);
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

    private void refreshVerifySignatureButton() {
        String disabledReason = getVerifySignatureDisabledReason();
        this.verifySignatureButton.setEnabled(disabledReason == null);
        String buttonDescription = disabledReason == null
                ? "Verify the JWT signature"
                : disabledReason + " to verify the signature";
        this.verifySignatureButton.setToolTipText(buttonDescription);
        this.verifySignatureButton.getAccessibleContext().setAccessibleDescription(buttonDescription);
        String missingInputHint = switch (disabledReason == null ? "" : disabledReason) {
            case "Enter the shared secret" -> "Enter a shared secret in Key / Secret";
            case "Enter the RSA public key" -> "Enter the RSA public key in Key / Secret";
            default -> null;
        };
        this.missingVerificationInputMessage.setText(missingInputHint == null ? "" : missingInputHint);
        this.missingVerificationInputMessage.setVisible(missingInputHint != null);
        refreshFeedbackPanel();
    }

    private String getVerifySignatureDisabledReason() {
        if (this.jwt.isEmpty()) {
            return "Paste a JWT";
        }
        String algorithm = this.jwt.getAlgorithm();
        if (!SignatureAlgorithm.isSupported(algorithm)) {
            return "Unsupported algorithm " + algorithm;
        }
        if (this.secretPanel instanceof RS256Panel rsaPanel) {
            return rsaPanel.snapshotKeyText().publicKey().isBlank() ? "Enter the RSA public key" : null;
        }
        return isValidatable() ? null : "Enter the shared secret";
    }

    private void showVerificationResult(String result, boolean warning) {
        this.signatureResultMessage.setText(result);
        this.signatureResultMessage.setToolTipText(result);
        this.signatureResultIcon.setIcon(warning ? AllIcons.General.BalloonWarning : AllIcons.General.InspectionsOK);
        this.signatureResultMessage.setForeground(warning
                ? this.signatureResultWarningForeground
                : JBColor.foreground());
        this.signatureResultPanel.setBackground(this.signatureResultWarningBackground);
        this.signatureResultPanel.setOpaque(warning);
        this.signatureResultPanel.setVisible(true);
        refreshFeedbackPanel();
    }

    private void clearSignatureResult() {
        this.signatureResultMessage.setText("");
        this.signatureResultMessage.setToolTipText(null);
        this.signatureResultIcon.setIcon(null);
        this.signatureResultPanel.setVisible(false);
        refreshFeedbackPanel();
    }

    private void onSignatureInputChanged() {
        clearSignatureResult();
        refreshVerifySignatureButton();
    }

    private void refreshJwt() {
        setJwt(this.jwt);
        this.jwtUpdateInProgress = true;
        this.propertyChangeSupport.firePropertyChange("jwt", null, this.jwt);
        this.jwtUpdateInProgress = false;
    }

    public void setJwt(Jwt jwt) {
        applyJwt(jwt, true);
        if (!jwt.isEmpty()) {
            this.parsedTabs.setSelectedIndex(0);
        }
    }

    public void clearParsedJwtKeepingSigningInput() {
        applyJwt(Jwt.EMPTY, false);
        if (this.secretPanel == this.unrecognizedSecretPanel) {
            this.unrecognizedSecretPanel.showUnsupportedAlgorithm(null);
        }
    }

    private void applyJwt(Jwt jwt, boolean updateSecretPanel) {
        if (this.jwtUpdateInProgress) {
            // This means that a change in the claims triggered
            // a change in String representation that
            // again triggered a change in tjeclaims.
            // In such a case, do nothing.
            return;
        }

        dismissClaimErrorTooltip();
        clearSignatureResult();
        this.jwt = jwt;
        this.headerTableModel = new JwtHeaderTableModel(jwt);
        this.headerTable.setModel(this.headerTableModel);
        this.headerTable.getEmptyText().setText(jwt.isEmpty() ? "No valid JWT to display" : "No header claims");
        this.headerTable.revalidate();
        this.headerTable.repaint();

        this.claimsTableModel = new JwtClaimsTableModel(jwt);
        applyClaimValidation(jwt);
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.getEmptyText().setText(jwt.isEmpty() ? "No valid JWT to display" : "No payload claims");
        this.claimsTableModel.addTableModelListener(e -> {
            dismissClaimErrorTooltip();
            updateClaimsTablePreferredSize();
        });
        updateClaimsTablePreferredSize();

        if (updateSecretPanel) {
            configureSecretPanel(jwt);
        }

        refreshVerifySignatureButton();
    }

    private void updateClaimsTablePreferredSize() {
        int rowCount = this.claimsTableModel != null ? this.claimsTableModel.getRowCount() : 0;
        int rows = rowCount == 0 ? 3 : rowCount;
        this.claimsTable.setPreferredScrollableViewportSize(new Dimension(0, rows * this.claimsTable.getRowHeight()));
        this.claimsTable.getParent().revalidate();
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
            this.unrecognizedSecretPanel.showUnsupportedAlgorithm(algorithm);
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

    private void onTimestampFormatChanged() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::onTimestampFormatChanged);
        } else if (!this.disposed) {
            dismissClaimErrorTooltip();
            refreshClaimValidation();
        }
    }

    private void applyClaimValidation(Jwt jwt) {
        if (Jwt.EMPTY.equals(jwt)) {
            this.claimsTableModel.setClaimErrors(Collections.emptyList());
            clearExpirationWarning();
            return;
        }
        DecodedJWT decodedJwt = jwt.getDecodedJwt();
        Instant now = this.clock.instant();
        Clock snapshotClock = Clock.fixed(now, this.clock.getZone());
        TimestampFormat format = Configuration.INSTANCE.getTimestampFormat();
        this.claimsTableModel.setClaimErrors(new JwtClaimsValidator(snapshotClock, format).validate(decodedJwt));
        updateClaimTimeState(decodedJwt, now, format);
    }

    private void clearExpirationWarning() {
        this.claimRefreshScheduler.cancel();
        updateExpirationWarning(null);
    }

    private void updateClaimTimeState(DecodedJWT decodedJwt, Instant reference, TimestampFormat format) {
        this.claimRefreshScheduler.cancel();
        BigDecimal now = NumericDate.fromInstant(reference);
        BigDecimal expiration = NumericDate.fromJson(
                decodedJwt.getClaim(RegisteredClaims.EXPIRES_AT).as(JsonNode.class));
        BigDecimal notBefore = NumericDate.fromJson(
                decodedJwt.getClaim(RegisteredClaims.NOT_BEFORE).as(JsonNode.class));
        boolean expired = expiration != null && now.compareTo(expiration) >= 0;
        updateExpirationWarning(expired ? JwtClaimsValidator.expirationMessage(expiration, format, reference) : null);

        BigDecimal deadline = earliestFutureDeadline(now, expiration, notBefore);
        if (deadline != null) {
            long delay = boundedRefreshDelayMillis(now, deadline);
            this.claimRefreshScheduler.schedule(delay, this::refreshClaimValidation);
        }
    }

    private void updateExpirationWarning(@Nullable String message) {
        this.expirationWarningMessage.setText(message == null ? "" : message);
        this.expirationWarningPanel.setVisible(message != null);
        refreshFeedbackPanel();
    }

    private static @Nullable BigDecimal earliestFutureDeadline(BigDecimal now, @Nullable BigDecimal expiration,
                                                               @Nullable BigDecimal notBefore) {
        BigDecimal deadline = expiration != null && now.compareTo(expiration) < 0 ? expiration : null;
        if (notBefore != null && now.compareTo(notBefore) < 0
                && (deadline == null || notBefore.compareTo(deadline) < 0)) {
            deadline = notBefore;
        }
        return deadline;
    }

    private static long boundedRefreshDelayMillis(BigDecimal now, BigDecimal deadline) {
        BigDecimal maximumDeadline = now.add(BigDecimal.valueOf(Integer.MAX_VALUE, 3));
        if (deadline.compareTo(maximumDeadline) >= 0) {
            return Integer.MAX_VALUE;
        }

        long lowerBound = 1;
        long upperBound = Integer.MAX_VALUE;
        while (lowerBound < upperBound) {
            long middle = lowerBound + (upperBound - lowerBound) / 2;
            BigDecimal candidate = now.add(BigDecimal.valueOf(middle, 3));
            if (candidate.compareTo(deadline) >= 0) {
                upperBound = middle;
            } else {
                lowerBound = middle + 1;
            }
        }
        return lowerBound;
    }

    private void refreshClaimValidation() {
        if (!this.disposed && !Jwt.EMPTY.equals(this.jwt)) {
            applyClaimValidation(this.jwt);
        }
    }

    private void refreshFeedbackPanel() {
        this.feedbackPanel.setVisible(this.expirationWarningPanel.isVisible() || this.signatureResultPanel.isVisible()
                || this.missingVerificationInputMessage.isVisible());
        JPanel parent = (JPanel) this.feedbackPanel.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }


    public void setSignatureContext(SignatureContext signatureContext) {
        this.secretPanel.removeSignatureContextChangedListener();
        try {
            this.secretPanel.setSignatureContext(signatureContext);
        } finally {
            configureSecretTextFieldListeners();
        }
        onSignatureInputChanged();
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

    JComponent getVerificationToolbarComponent() {
        return this.verificationToolbarComponent;
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
        this.parsedTabs.setSelectedIndex(0);
        for (SecretPanel panel : this.secretPanels.values()) {
            Disposer.dispose(panel);
        }
        this.secretPanels.clear();
        this.rsaDefaultsText = null;
        this.rsaDefaultsGeneration++;
    }

    public void addJwtListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener("jwt", listener);
    }

    private final class AddClaimAction extends AddClaimActionButtonController {
        private AddClaimAction() {
            super(JwtPanel.this::isAddClaimActionEnabled);
        }

        @Override
        public void onClaimTypeSelected(DataTypeRegistry.DataType dataType) {
            onNewAction(dataType);
        }
    }

    private final class CopyPayloadAsJsonAction extends AnAction {
        private CopyPayloadAsJsonAction() {
            super("Copy payload as JSON", null, AllIcons.FileTypes.Json);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            onCopyAsJsonActionPerformed(e);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!JwtPanel.this.jwt.getPayloadClaims().isEmpty());
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private final class CopyClaimAction extends AnAction {
        private final Runnable handler;

        private CopyClaimAction(String text, Runnable handler) {
            super(text);
            this.handler = handler;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            this.handler.run();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(JwtPanel.this.claimsTable.getSelectedRow() >= 0);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private final class CopyHeaderValueAction extends AnAction {
        private CopyHeaderValueAction() {
            super("Copy value");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            copySelectedHeaderValue();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(JwtPanel.this.headerTable.getSelectedRow() >= 0);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private static final class ClaimValueTable extends JBTable {
        private boolean initialWidthsApplied;

        @Override
        public void doLayout() {
            if (!this.initialWidthsApplied && getWidth() >= JBUI.scale(200) && getColumnCount() == 2) {
                int claimWidth = Math.max(JBUI.scale(65), Math.round(getWidth() * 0.3f));
                int valueWidth = getWidth() - claimWidth;
                TableColumnModel columns = getColumnModel();
                columns.getColumn(0).setPreferredWidth(claimWidth);
                columns.getColumn(0).setWidth(claimWidth);
                columns.getColumn(1).setPreferredWidth(valueWidth);
                columns.getColumn(1).setWidth(valueWidth);
                this.initialWidthsApplied = true;
            }
            super.doLayout();
        }

        @Override
        public Point getPopupLocation(MouseEvent event) {
            if (event != null) {
                int row = rowAtPoint(event.getPoint());
                if (row >= 0) {
                    setRowSelectionInterval(row, row);
                } else {
                    clearSelection();
                }
            }
            return super.getPopupLocation(event);
        }
    }

    interface ClaimRefreshScheduler {
        void schedule(long delayMillis, Runnable task);

        void cancel();
    }

    private static final class SwingClaimRefreshScheduler implements ClaimRefreshScheduler {
        private Timer timer;

        private long generation;

        @Override
        public void schedule(long delayMillis, Runnable task) {
            cancel();
            long scheduledGeneration = ++this.generation;
            int delay = (int) Math.max(1L, Math.min(delayMillis, Integer.MAX_VALUE));
            this.timer = new Timer(delay, event -> {
                if (scheduledGeneration != this.generation) {
                    return;
                }
                Timer timer = this.timer;
                this.timer = null;
                if (timer != null) {
                    timer.stop();
                }
                task.run();
            });
            this.timer.setRepeats(false);
            this.timer.start();
        }

        @Override
        public void cancel() {
            this.generation++;
            Timer timer = this.timer;
            this.timer = null;
            if (timer != null) {
                timer.stop();
            }
        }
    }

    @Override
    public void dispose() {
        this.disposed = true;
        this.claimRefreshScheduler.cancel();
        dismissClaimErrorTooltip();
        this.secretPanel.removeSignatureContextChangedListener();
        this.secretPanels.clear();
    }
}

