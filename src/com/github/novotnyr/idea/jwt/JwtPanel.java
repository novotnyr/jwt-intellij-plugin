package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JwtPanel extends JPanel {
    private JLabel headerLabel = new JLabel("Header");

    private JwtHeaderTableModel headerTableModel;

    private JBTable headerTable = new JBTable();

    private JLabel payloadLabel = new JLabel("Payload");

    private JWTClaimsTableModel claimsTableModel;

    private JBTable claimsTable = new JBTable();

    private JLabel verifySignatureLabel = new JLabel("Verify signature with secret:");

    private JTextField secretTextField = new JTextField();

    private JButton validateButton = new JButton("Validate");

    private DecodedJWT jwt;

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

        cc.gridy++;
        add(this.headerTable, cc);

        cc.gridy++;
        add(this.payloadLabel, cc);

        cc.gridy++;
        cc.weighty = 1;
        add(this.claimsTable, cc);

        cc.gridy++;
        cc.weighty = 0;
        add(this.verifySignatureLabel, cc);

        cc.gridy++;
        add(this.secretTextField, cc);

        cc.gridy++;
        add(this.validateButton, cc);
        this.validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onValidateButtonClick(e);
            }
        });

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

    public void setJwt(DecodedJWT jwt) {
        this.jwt = jwt;

        this.headerTableModel = new JwtHeaderTableModel(jwt);
        this.headerTable.setModel(this.headerTableModel);

        this.claimsTableModel = new JWTClaimsTableModel(jwt);
        this.claimsTable.setModel(this.claimsTableModel);
        this.claimsTable.setDefaultRenderer(Object.class, this.claimsTableModel);
    }

    public String getSecret() {
        return this.secretTextField.getText();
    }

    public JTextField getSecretTextField() {
        return secretTextField;
    }
}
