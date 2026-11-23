package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.github.novotnyr.idea.jwt.TimestampFormatter;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jspecify.annotations.NonNull;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.Instant;

public class DateClaimPanel extends AbstractClaimPanel<DateClaim, BigDecimal> {

    private static final BigDecimal MIN_MILLISECOND_TIMESTAMP = BigDecimal.valueOf(100_000_000_000L);

    private static final BigDecimal MAX_MILLISECOND_TIMESTAMP = BigDecimal.valueOf(100_000_000_000_000L);

    private JBLabel datePreviewLabel;

    private JBLabel localDatePreviewLabel;

    private JButton nowButton;

    private JButton add15MinutesButton;

    private JButton subtract15MinutesButton;

    private JPanel shortcutPanel;

    public DateClaimPanel(DateClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.claimValueTextField.setText(this.value.getValueString());
        this.claimValueTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.claimValueTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                this.claimValueTextField.getPreferredSize().height));

        add(this.claimValueTextField);
        add(this.datePreviewLabel = createInlineHelpTextLabel());
        add(this.localDatePreviewLabel = createInlineHelpTextLabel());
        this.datePreviewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.localDatePreviewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.shortcutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.shortcutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.shortcutPanel.add(this.nowButton = new JButton("Now"));
        this.shortcutPanel.add(this.subtract15MinutesButton = new JButton("-15 min"));
        this.shortcutPanel.add(this.add15MinutesButton = new JButton("+15 min"));
        add(this.shortcutPanel);

        this.claimValueTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NonNull DocumentEvent documentEvent) {
                onClaimValueTextFieldChanged();
            }
        });
        onClaimValueTextFieldChanged();

        this.nowButton.addActionListener(this::nowButtonClicked);
        this.add15MinutesButton.addActionListener(this::add15MinutesButtonClicked);

        this.subtract15MinutesButton.addActionListener(this::subtract15MinutesButtonClicked);
    }

    private static JBLabel createInlineHelpTextLabel() {
        return new JBLabel("", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    }

    @Override
    public int getBaseline(int width, int height) {
        return this.claimValueTextField.getBaseline(width, this.claimValueTextField.getPreferredSize().height);
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
        this.localDatePreviewLabel.setVisible(false);
        try {
            this.validationInfo = null;
            BigDecimal timestamp = new BigDecimal(this.claimValueTextField.getText());
            this.add15MinutesButton.setEnabled(!this.readOnly);
            this.subtract15MinutesButton.setEnabled(!this.readOnly);
            Instant preview = NumericDate.toInstant(timestamp);
            if (preview == null) {
                this.datePreviewLabel.setText(addMillisecondsHint("UTC: Timestamp outside display range", timestamp));
                return;
            }
            String utcPreview = preview.toString();
            this.datePreviewLabel.setText(addMillisecondsHint("UTC: " + utcPreview, timestamp));
            String localPreview = TimestampFormatter.formatLocalIso(preview);
            if (localPreview != null && !utcPreview.equals(localPreview)) {
                this.localDatePreviewLabel.setText("Local: " + localPreview);
                this.localDatePreviewLabel.setVisible(true);
            }
        } catch (NumberFormatException e) {
            this.datePreviewLabel.setText("UTC: Not a valid timestamp");
            this.validationInfo = new ValidationInfo("Not a valid timestamp", this.claimValueTextField);
            this.add15MinutesButton.setEnabled(false);
            this.subtract15MinutesButton.setEnabled(false);
        }
    }

    private static String addMillisecondsHint(String preview, BigDecimal timestamp) {
        BigDecimal magnitude = timestamp.abs();
        if (magnitude.compareTo(MIN_MILLISECOND_TIMESTAMP) >= 0
                && magnitude.compareTo(MAX_MILLISECOND_TIMESTAMP) <= 0) {
            return preview + " · JWT timestamps use seconds. Did you paste milliseconds?";
        }
        return preview;
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
        onClaimValueTextFieldChanged();
    }
}
