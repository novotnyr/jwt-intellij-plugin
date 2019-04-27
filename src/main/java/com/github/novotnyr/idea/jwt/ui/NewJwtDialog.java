package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class NewJwtDialog extends DialogWrapper {
    private final JwtFactory jwtFactory = JwtFactory.getInstance();

    private JTextField algorithmTextField;
    private JTextField signingSecretTextField;
    private JPanel rootPanel;
    private JCheckBox addIatCheckBox;

    public NewJwtDialog(@Nullable Project project) {
        super(project);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.rootPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (StringUtils.isEmpty(signingSecretTextField.getText())) {
            return new ValidationInfo("Please provide signing secret for JWT", this.signingSecretTextField);
        }
        return super.doValidate();
    }

    public Jwt getJwt() {
        return this.jwtFactory.newJwt(getAlgorithm(), getSignatureContext(), isAddingIat());
    }

    private String getAlgorithm() {
        return this.algorithmTextField.getText();
    }

    public SignatureContext getSignatureContext() {
        return new HS256SignatureContext(this.signingSecretTextField.getText());
    }

    public boolean isAddingIat() {
        return this.addIatCheckBox.isSelected();
    }

}
