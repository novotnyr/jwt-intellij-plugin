package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.DateUtils;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class DateClaimPanel extends AbstractClaimPanel<DateClaim, Date> {

    private JLabel datePreviewLabel;

    private JButton nowButton;

    private JButton add15MinutesButton;

    private JButton subtract15MinutesButton;

    private ValidationInfo validationInfo;

    public DateClaimPanel(DateClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.claimValueTextField.setText(value.getValueString());

        add(this.claimValueTextField);
        add(this.datePreviewLabel = new JLabel());
        add(this.nowButton = new JButton("Now"));
        add(this.add15MinutesButton = new JButton("+15 min"));
        add(this.subtract15MinutesButton = new JButton("-15 min"));

        this.claimValueTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                onClaimValueTextFieldChanged();
            }
        });
        onClaimValueTextFieldChanged();

        this.nowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nowButtonClicked(e);
            }
        });
        this.add15MinutesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                add15MinutesButtonClicked(e);
            }
        });

        this.subtract15MinutesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                subtract15MinutesButtonClicked(e);
            }
        });
    }

    private void nowButtonClicked(ActionEvent e) {
        validationInfo = null;
        long unixTimestamp = new Date().getTime() / 1000;
        claimValueTextField.setText(String.valueOf(unixTimestamp));
        datePreviewLabel.setText(new Date(unixTimestamp * 1000).toString());
    }

    private void add15MinutesButtonClicked(ActionEvent event) {
        try {
            long unixTimestamp = Long.parseLong(claimValueTextField.getText());
            unixTimestamp = unixTimestamp + (15 * 60);
            claimValueTextField.setText(String.valueOf(unixTimestamp));
            datePreviewLabel.setText(new Date(unixTimestamp * 1000).toString());
        } catch (NumberFormatException e) {
            datePreviewLabel.setText("Not a valid timestamp");
            validationInfo = new ValidationInfo("Not a valid timestamp", claimValueTextField);
        }
    }

    private void subtract15MinutesButtonClicked(ActionEvent event) {
        try {
            long unixTimestamp = Long.parseLong(claimValueTextField.getText());
            unixTimestamp = unixTimestamp - (15 * 60);
            claimValueTextField.setText(String.valueOf(unixTimestamp));
            datePreviewLabel.setText(new Date(unixTimestamp * 1000).toString());
        } catch (NumberFormatException e) {
            datePreviewLabel.setText("Not a valid timestamp");
            validationInfo = new ValidationInfo("Not a valid timestamp", claimValueTextField);
        }
    }

    private void onClaimValueTextFieldChanged() {
        try {
            validationInfo = null;
            long unixTimestamp = Long.parseLong(claimValueTextField.getText());
            datePreviewLabel.setText(new Date(unixTimestamp * 1000).toString());
        } catch (NumberFormatException e) {
            datePreviewLabel.setText("Not a valid timestamp");
            validationInfo = new ValidationInfo("Not a valid timestamp", claimValueTextField);
        }
    }

    @Override
    public Date getClaimValue() {
        return DateUtils.toDate(this.claimValueTextField.getText());
    }

    public ValidationInfo getValidationInfo() {
        return validationInfo;
    }

    public void setValidationInfo(ValidationInfo validationInfo) {
        this.validationInfo = validationInfo;
    }
}
