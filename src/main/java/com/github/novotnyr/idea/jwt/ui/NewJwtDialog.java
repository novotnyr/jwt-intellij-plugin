package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.SecretPanelFactory;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RsaKeyParser;
import com.github.novotnyr.idea.jwt.rs256.RsaKeyValidationException;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class NewJwtDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(NewJwtDialog.class);

    private record AcceptedJwt(Jwt jwt, SignatureContext signatureContext) {
    }

    private final JwtFactory jwtFactory = JwtFactory.getInstance();

    /**
     * Runs configured RSA key loading and OK-time key parsing and signing off the EDT.
     * Completion handlers return to the EDT and discard results for stale or disposed dialog state.
     */
    private final Executor rsaExecutor;

    private final Map<String, SecretPanel> secretPanels = new HashMap<>();

    @Nullable
    private final Project project;

    private static final String[] ALGORITHMS = {"HS256", "RS256", "HS384"};

    private JComboBox<String> algorithmComboBox;
    private JPanel rootPanel;
    private JCheckBox addIatCheckBox;
    private JPanel secretPanelContainer;
    private SecretPanel secretPanel;
    private Jwt acceptedJwt;
    private SignatureContext acceptedSignatureContext;
    private RS256Panel.KeyTextSnapshot invalidRsaText;
    private ValidationInfo rsaValidationError;
    private long rsaValidationGeneration;
    private boolean rsaValidationInProgress;

    public NewJwtDialog(@Nullable Project project) {
        this(project, AppExecutorUtil.getAppExecutorService());
    }

    NewJwtDialog(@Nullable Project project, Executor rsaExecutor) {
        super(project);

        this.rsaExecutor = rsaExecutor;
        this.project = project;

        this.algorithmComboBox.setModel(new DefaultComboBoxModel<>(ALGORITHMS));
        this.algorithmComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                onAlgorithmComboBoxItemSelected((String) event.getItem());
            }
        });
        this.algorithmComboBox.setSelectedItem("HS256");
        onAlgorithmComboBoxItemSelected("HS256");
        init();
    }

    private void onAlgorithmComboBoxItemSelected(String algorithmName) {
        SecretPanel selectedPanel = this.secretPanels.get(algorithmName);
        if (selectedPanel == null) {
            selectedPanel = SecretPanelFactory.create(this.project, algorithmName, SignatureContext.EMPTY);
            Disposer.register(getDisposable(), selectedPanel);
            this.secretPanels.put(algorithmName, selectedPanel);
            if (selectedPanel instanceof RS256Panel rsaPanel) {
                rsaPanel.addKeyTextChangeListener(new DocumentListener() {
                    @Override
                    public void documentChanged(@NonNull DocumentEvent event) {
                        invalidateRsaValidation();
                    }
                }, getDisposable());
                loadDefaultRsaKeys(rsaPanel);
            }
        }
        if (this.secretPanel != selectedPanel) {
            invalidateRsaValidation();
        }
        this.secretPanel = selectedPanel;
        this.secretPanelContainer.removeAll();
        this.secretPanelContainer.add(this.secretPanel.getRoot(), BorderLayout.CENTER);
        this.secretPanelContainer.revalidate();
        this.secretPanelContainer.repaint();
    }

    private void loadDefaultRsaKeys(RS256Panel panel) {
        IdePreferenceNewSignatureContextProvider provider = new IdePreferenceNewSignatureContextProvider(this.project);
        RS256Panel.KeyTextSnapshot initialText = panel.snapshotKeyText();
        CompletableFuture.supplyAsync(() -> provider
                        .<SignatureContext>createSignatureContext("RS256"), this.rsaExecutor)
                .whenComplete((context, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        LOG.warn("Cannot load default RSA keys", error);
                        return;
                    }
                    if (!isDisposed() && this.secretPanels.get("RS256") == panel
                            && panel.snapshotKeyText().equals(initialText)
                            && context instanceof RS256SignatureContext) {
                        panel.setSignatureContext(context);
                    }
                }));
    }

    private void invalidateRsaValidation() {
        if (!this.rsaValidationInProgress && this.rsaValidationError == null) {
            return;
        }
        this.rsaValidationGeneration++;
        this.rsaValidationInProgress = false;
        this.rsaValidationError = null;
        this.invalidRsaText = null;
        if (!isDisposed()) {
            setErrorInfoAll(List.of());
            setOKActionEnabled(true);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.rootPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (MUTABLE != this.secretPanel.getStatus()) {
            return new ValidationInfo("Missing secret or keypair", this.secretPanel.getBaloonableComponent());
        }
        if (this.secretPanel instanceof RS256Panel rsaPanel) {
            if (this.rsaValidationInProgress) {
                return new ValidationInfo("Validating RSA keys", rsaPanel.getRoot());
            }
            if (this.rsaValidationError != null
                    && rsaPanel.snapshotKeyText().equals(this.invalidRsaText)) {
                return this.rsaValidationError;
            }
        }
        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        ValidationInfo validation = doValidate();
        if (validation != null) {
            setErrorInfoAll(List.of(validation));
            return;
        }

        if (this.secretPanel instanceof RS256Panel rsaPanel) {
            validateRsaAndAccept(rsaPanel);
            return;
        }
        SignatureContext signatureContext = this.secretPanel.getSignatureContext();
        accept(this.jwtFactory.newJwt(getAlgorithm(), signatureContext, isAddingIat()), signatureContext);
    }

    private void validateRsaAndAccept(RS256Panel panel) {
        RS256Panel.KeyTextSnapshot snapshot = panel.snapshotKeyText();
        boolean addIat = isAddingIat();
        long generation = ++this.rsaValidationGeneration;
        this.rsaValidationInProgress = true;
        setOKActionEnabled(false);
        CompletableFuture.supplyAsync(() -> {
            SignatureContext context = RsaKeyParser.parse(snapshot.publicKey(), snapshot.privateKey());
            return new AcceptedJwt(this.jwtFactory.newJwt("RS256", context, addIat), context);
        }, this.rsaExecutor).whenComplete((result, error) ->
                SwingUtilities.invokeLater(() -> finishRsaValidation(panel, snapshot, addIat, generation, result, error)));
    }

    private void finishRsaValidation(RS256Panel panel, RS256Panel.KeyTextSnapshot snapshot,
                                     boolean addIat, long generation, AcceptedJwt result, Throwable error) {
        if (isDisposed() || generation != this.rsaValidationGeneration) {
            return;
        }
        this.rsaValidationInProgress = false;
        setOKActionEnabled(true);
        if (this.secretPanel != panel || !panel.snapshotKeyText().equals(snapshot) || isAddingIat() != addIat) {
            return;
        }
        if (error != null) {
            Throwable cause = error instanceof CompletionException ? error.getCause() : error;
            if (!(cause instanceof RsaKeyValidationException keyError)) {
                LOG.error("Cannot create RSA JWT", cause);
                setErrorInfoAll(List.of());
                return;
            }
            JComponent input = keyError.getField() == RsaKeyValidationException.Field.PUBLIC
                    ? panel.getPublicKeyInput() : panel.getPrivateKeyInput();
            this.invalidRsaText = snapshot;
            this.rsaValidationError = new ValidationInfo(keyError.getMessage(), input);
            setErrorInfoAll(List.of(this.rsaValidationError));
            return;
        }
        this.invalidRsaText = null;
        this.rsaValidationError = null;
        setErrorInfoAll(List.of());
        accept(result.jwt(), result.signatureContext());
    }

    private void accept(Jwt jwt, SignatureContext signatureContext) {
        super.doOKAction();
        if (getExitCode() == OK_EXIT_CODE) {
            this.acceptedSignatureContext = signatureContext;
            this.acceptedJwt = jwt;
        }
    }

    /**
     * Returns the JWT created when this dialog was accepted.
     *
     * @return the accepted JWT
     * @throws NewJwtDialogNotAcceptedException if the dialog has not been accepted
     */
    public Jwt getJwt() throws NewJwtDialogNotAcceptedException {
        if (this.acceptedJwt == null) {
            throw new NewJwtDialogNotAcceptedException();
        }
        return this.acceptedJwt;
    }

    private String getAlgorithm() {
        return (String) this.algorithmComboBox.getSelectedItem();
    }

    /**
     * Returns the signature context selected when this dialog was accepted.
     *
     * @return the accepted signature context
     * @throws NewJwtDialogNotAcceptedException if the dialog has not been accepted
     */
    public SignatureContext getSignatureContext() throws NewJwtDialogNotAcceptedException {
        if (this.acceptedSignatureContext == null) {
            throw new NewJwtDialogNotAcceptedException();
        }
        return this.acceptedSignatureContext;
    }

    public boolean isAddingIat() {
        return this.addIatCheckBox.isSelected();
    }

}
