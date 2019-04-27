package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.SecretPanelFactory;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class NewJwtDialog extends DialogWrapper {
    private final JwtFactory jwtFactory = JwtFactory.getInstance();

    private static final String[] ALGORITHMS = {"HS256", "RS256"};

    private JComboBox<String> algorithmComboBox;
    private JPanel rootPanel;
    private JCheckBox addIatCheckBox;
    private JPanel secretPanelContainer;
    private SecretPanel secretPanel;

    public NewJwtDialog(@Nullable Project project) {
        super(project);
        algorithmComboBox.setModel(new DefaultComboBoxModel<>(ALGORITHMS));
        algorithmComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    onAlgorithmComboBoxItemSelected((String) event.getItem());
                }
            }
        });
        algorithmComboBox.setSelectedItem("HS256");
        onAlgorithmComboBoxItemSelected("HS256");
        init();
    }

    private void onAlgorithmComboBoxItemSelected(String algorithmName) {
        this.secretPanel = SecretPanelFactory.getInstance().newSecretPanel(algorithmName);
        this.secretPanelContainer.removeAll();
        this.secretPanelContainer.add(secretPanel.getRoot(), BorderLayout.CENTER);
        this.secretPanelContainer.revalidate();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.rootPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (this.secretPanel.hasSecret()) {
            return super.doValidate();
        }
        ValidationInfo validationInfo = new ValidationInfo("Missing secret", this.secretPanel.getBaloonableComponent());
        return validationInfo;
    }

    public Jwt getJwt() {
        return this.jwtFactory.newJwt(getAlgorithm(), getSignatureContext(), isAddingIat());
    }

    private String getAlgorithm() {
        return (String) this.algorithmComboBox.getSelectedItem();
    }

    public SignatureContext getSignatureContext() {
        return this.secretPanel.getSignatureContext();
    }

    public boolean isAddingIat() {
        return this.addIatCheckBox.isSelected();
    }

}
