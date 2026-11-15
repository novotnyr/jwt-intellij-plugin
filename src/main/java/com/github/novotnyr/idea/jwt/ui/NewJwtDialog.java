package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.NewSignatureContextProvider;
import com.github.novotnyr.idea.jwt.SecretPanelFactory;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class NewJwtDialog extends DialogWrapper {
    private final JwtFactory jwtFactory = JwtFactory.getInstance();

    private final NewSignatureContextProvider newSignatureContextProvider;

    private final Function<String, SecretPanel> panelCopyProvider;

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

    public NewJwtDialog(@Nullable Project project) {
        this(project, algorithm -> null);
    }

    public NewJwtDialog(@Nullable Project project, Function<String, SecretPanel> panelCopyProvider) {
        super(project);

        this.newSignatureContextProvider = new IdePreferenceNewSignatureContextProvider(project);
        this.panelCopyProvider = panelCopyProvider;
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
            selectedPanel = this.panelCopyProvider.apply(algorithmName);
            if (selectedPanel == null) {
                SignatureContext initialSignatureContext = this.newSignatureContextProvider.createSignatureContext(algorithmName);
                selectedPanel = SecretPanelFactory.create(this.project, algorithmName, initialSignatureContext);
            }
            Disposer.register(getDisposable(), selectedPanel);
            this.secretPanels.put(algorithmName, selectedPanel);
        }
        this.secretPanel = selectedPanel;
        this.secretPanelContainer.removeAll();
        this.secretPanelContainer.add(this.secretPanel.getRoot(), BorderLayout.CENTER);
        this.secretPanelContainer.revalidate();
        this.secretPanelContainer.repaint();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.rootPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (MUTABLE == this.secretPanel.getStatus()) {
            return super.doValidate();
        }
        return new ValidationInfo("Missing secret or keypair", this.secretPanel.getBaloonableComponent());
    }

    @Override
    protected void doOKAction() {
        if (doValidate() != null) {
            return;
        }

        SignatureContext signatureContext = this.secretPanel.getSignatureContext();
        Jwt jwt = this.jwtFactory.newJwt(getAlgorithm(), signatureContext, isAddingIat());
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
