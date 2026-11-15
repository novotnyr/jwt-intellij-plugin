package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

public class DateClaimPanel extends AbstractClaimPanel<DateClaim, BigDecimal> {

    private JLabel datePreviewLabel;

    private JButton nowButton;

    private JButton add15MinutesButton;

    private JButton subtract15MinutesButton;

    public DateClaimPanel(DateClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.claimValueTextField.setText(this.value.getValueString());

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

        this.nowButton.addActionListener(this::nowButtonClicked);
        this.add15MinutesButton.addActionListener(this::add15MinutesButtonClicked);

        this.subtract15MinutesButton.addActionListener(this::subtract15MinutesButtonClicked);
    }

    private void nowButtonClicked(ActionEvent e) {
        this.claimValueTextField.setText(Long.toString(Instant.now().getEpochSecond()));
    }

    private void add15MinutesButtonClicked(ActionEvent event) {
        shiftSeconds(15 * 60);
    }

    private void subtract15MinutesButtonClicked(ActionEvent event) {
        shiftSeconds(-15 * 60);
    }

    private void shiftSeconds(int seconds) {
        try {
            BigDecimal timestamp = new BigDecimal(this.claimValueTextField.getText());
            this.claimValueTextField.setText(timestamp.add(BigDecimal.valueOf(seconds)).toString());
        } catch (NumberFormatException e) {
            onClaimValueTextFieldChanged();
        }
    }

    private void onClaimValueTextFieldChanged() {
        try {
            this.validationInfo = null;
            BigDecimal timestamp = new BigDecimal(this.claimValueTextField.getText());
            Date preview = NumericDate.toDate(timestamp);
            this.datePreviewLabel.setText(preview == null ? "Timestamp outside display range" : preview.toString());
        } catch (NumberFormatException e) {
            this.datePreviewLabel.setText("Not a valid timestamp");
            this.validationInfo = new ValidationInfo("Not a valid timestamp", this.claimValueTextField);
        }
    }

    @Override
    public BigDecimal getClaimValue() {
        return new BigDecimal(this.claimValueTextField.getText());
    }

    public ValidationInfo getValidationInfo() {
        return this.validationInfo;
    }

    public void setValidationInfo(ValidationInfo validationInfo) {
        this.validationInfo = validationInfo;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.nowButton.setEnabled(!readOnly);
        this.add15MinutesButton.setEnabled(!readOnly);
        this.subtract15MinutesButton.setEnabled(!readOnly);
    }
}
