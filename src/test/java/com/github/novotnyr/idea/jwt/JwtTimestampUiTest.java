package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.ui.claimpanel.DateClaimPanel;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class JwtTimestampUiTest extends LightPlatformTestCase {
    public void testClaimErrorsNotifyRowsWithoutClearingSelection() {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setPayloadClaim(new StringClaim("subject", "alice"));
        JwtClaimsTableModel model = new JwtClaimsTableModel(jwt);
        JTable table = new JTable(model);
        table.setRowSelectionInterval(0, 0);
        List<TableModelEvent> changes = new ArrayList<>();
        model.addTableModelListener(changes::add);

        model.setClaimErrors(List.of(new ClaimError("subject", "Example error")));

        assertEquals(1, changes.size());
        assertEquals(TableModelEvent.UPDATE, changes.getFirst().getType());
        assertEquals(0, changes.getFirst().getFirstRow());
        assertEquals(0, changes.getFirst().getLastRow());
        assertEquals(0, table.getSelectedRow());
    }

    public void testTimestampErrorMatchesOnlyExactClaimName() {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setPayloadClaim(new StringClaim("iat", "yes"));
        jwt.setPayloadClaim(new StringClaim("IAT", "custom value"));
        JwtClaimsTableModel model = new JwtClaimsTableModel(jwt);
        model.setClaimErrors(new JwtValidator().validateClaims(jwt).claimErrors());

        ClaimError error = model.getClaimErrorAt(0);
        assertNotNull(error);
        assertEquals("iat", error.getClaim());
        assertNull(model.getClaimErrorAt(1));
        assertNull(model.getClaimErrorAt(-1));
        assertNull(model.getClaimErrorAt(2));
        assertEquals("yes", model.getValueAt(0, 1));
    }

    public void testTimestampEditorAcceptsFractionalSeconds() {
        DateClaim claim = DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.125"));
        DateClaimPanel panel = new DateClaimPanel(claim);
        JTextField valueField = findComponent(panel, JTextField.class);
        JLabel datePreview = findComponent(panel, JLabel.class);
        JButton add15Minutes = findButton(panel, "+15 min");
        assertNotNull(valueField);
        assertNotNull(datePreview);
        assertNotNull(add15Minutes);
        assertEquals("1735689601.125", valueField.getText());
        assertNull(panel.getValidationInfo());
        assertEquals("UTC: 2025-01-01T00:00:01.125Z", datePreview.getText());
        assertEquals(0, claim.getValue().compareTo(panel.getClaimValue()));

        add15Minutes.doClick();
        assertEquals(0, new BigDecimal("1735690501.125").compareTo(panel.getClaimValue()));
        assertEquals("UTC: 2025-01-01T00:15:01.125Z", datePreview.getText());
        assertNull(panel.getValidationInfo());

        valueField.setText("999999999999999999999999");
        assertEquals("Date preview unavailable", datePreview.getText());
        assertTrue(add15Minutes.isEnabled());

        valueField.setText("not a number");
        assertFalse(datePreview.isVisible());
        assertEquals(List.of(), visiblePreviews(panel));
        assertFalse(add15Minutes.isEnabled());
        assertNotNull(panel.getValidationInfo());

        JButton now = findButton(panel, "Now");
        assertTrue(now.isEnabled());
        now.doClick();
        assertNull(panel.getValidationInfo());
        assertTrue(new BigDecimal(valueField.getText()).compareTo(BigDecimal.ZERO) > 0);

        panel.setReadOnly(true);
        assertFalse(now.isEnabled());
        assertFalse(add15Minutes.isEnabled());
    }

    public void testMillisecondTimestampShowsNonBlockingHintWithoutChangingInput() {
        DateClaimPanel panel = new DateClaimPanel(DateClaim.fromNumericDate("exp", new BigDecimal("1735689601")));
        JTextField valueField = findComponent(panel, JTextField.class);
        JButton add15Minutes = findButton(panel, "+15 min");
        JLabel datePreview = findComponent(panel, JLabel.class);

        valueField.setText("1735689601000");

        assertEquals("1735689601000", valueField.getText());
        assertNull(panel.getValidationInfo());
        assertTrue(add15Minutes.isEnabled());
        assertTrue(datePreview.getText().contains("JWT timestamps use seconds. Did you paste milliseconds?"));

        valueField.setText("1735689601");

        assertEquals("UTC: 2025-01-01T00:00:01Z", datePreview.getText());
        assertFalse(datePreview.getText().contains("milliseconds"));
    }

    public void testTimestampFieldAndShortcutsKeepCompactLayout() {
        DateClaimPanel panel = new DateClaimPanel(DateClaim.fromNumericDate("exp", BigDecimal.ZERO));
        JTextField valueField = findComponent(panel, JTextField.class);
        JButton now = findButton(panel, "Now");
        JButton add15Minutes = findButton(panel, "+15 min");
        JButton subtract15Minutes = findButton(panel, "-15 min");

        assertNotNull(valueField);
        assertSame(add15Minutes.getParent(), subtract15Minutes.getParent());
        assertSame(now.getParent(), add15Minutes.getParent());
        Dimension preferredSize = valueField.getPreferredSize();
        panel.setSize(400, 300);
        panel.doLayout();
        assertEquals(preferredSize.height, valueField.getHeight());
    }

    public void testTimestampPreviewsUseInlineHelpTextStyle() {
        DateClaimPanel panel = new DateClaimPanel(DateClaim.fromNumericDate("exp", BigDecimal.ZERO));
        List<JLabel> previews = Arrays.stream(panel.getComponents())
                .filter(JLabel.class::isInstance)
                .map(JLabel.class::cast)
                .toList();

        assertEquals(2, previews.size());
        for (JLabel preview : previews) {
            assertTrue(preview instanceof JBLabel);
            JBLabel inlineHelpText = (JBLabel) preview;
            assertEquals(UIUtil.ComponentStyle.SMALL, inlineHelpText.getComponentStyle());
            assertEquals(UIUtil.FontColor.BRIGHTER, inlineHelpText.getFontColor());
        }
    }

    public void testTimestampLabelStaysAlignedWithFieldWhenDialogResizes() {
        ClaimDialog dialog = new ClaimDialog(DateClaim.fromNumericDate("iat", BigDecimal.ZERO),
                ClaimDialog.Mode.EDIT, null);
        Container content = (Container) dialog.createCenterPanel();
        Dimension preferredSize = content.getPreferredSize();
        content.setSize(preferredSize.width + 300, preferredSize.height + 200);
        layoutRecursively(content);

        JLabel timestampLabel = findLabelWithText(content, "Unix timestamp:");
        DateClaimPanel datePanel = findComponentRecursively(content, DateClaimPanel.class);
        assertNotNull(timestampLabel);
        assertNotNull(datePanel);
        JTextField valueField = datePanel.getTimestampTextField();
        assertNotNull(valueField);
        assertSame(valueField, timestampLabel.getLabelFor());
        assertSame(valueField, dialog.getPreferredFocusedComponent());

        int labelBaseline = timestampLabel.getBaseline(timestampLabel.getWidth(), timestampLabel.getHeight());
        int fieldBaseline = valueField.getBaseline(valueField.getWidth(), valueField.getHeight());
        assertTrue(labelBaseline >= 0);
        assertTrue(fieldBaseline >= 0);
        Point labelPoint = SwingUtilities.convertPoint(timestampLabel, 0, labelBaseline, content);
        Point fieldPoint = SwingUtilities.convertPoint(valueField, 0, fieldBaseline, content);
        assertEquals(labelPoint.y, fieldPoint.y);
    }

    public void testTimestampFieldIsNotPreferredFocusInNewOrViewMode() {
        DateClaim claim = DateClaim.fromNumericDate("iat", BigDecimal.ZERO);
        ClaimDialog newDialog = new ClaimDialog(claim, ClaimDialog.Mode.NEW, null);
        ClaimDialog viewDialog = new ClaimDialog(claim, ClaimDialog.Mode.VIEW, null);
        DateClaimPanel newDatePanel = findComponentRecursively(newDialog.createCenterPanel(), DateClaimPanel.class);
        DateClaimPanel viewDatePanel = findComponentRecursively(viewDialog.createCenterPanel(), DateClaimPanel.class);
        assertNotNull(newDatePanel);
        assertNotNull(viewDatePanel);

        JTextField newValueField = newDatePanel.getTimestampTextField();
        JTextField viewValueField = viewDatePanel.getTimestampTextField();
        assertNotNull(newValueField);
        assertNotNull(viewValueField);
        assertNotSame(newValueField, newDialog.getPreferredFocusedComponent());
        assertNotSame(viewValueField, viewDialog.getPreferredFocusedComponent());
    }

    public void testTimestampEditorShowsUtcAndUpdatesLocalPreview() {
        TimeZone originalZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            DateClaimPanel panel = new DateClaimPanel(DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.125")));
            JTextField valueField = findComponent(panel, JTextField.class);
            JButton add15Minutes = findButton(panel, "+15 min");
            assertEquals(List.of("UTC: 2025-01-01T00:00:01.125Z", "Local: 2024-12-31T19:00:01.125-05:00"),
                    visiblePreviews(panel));

            valueField.setText("1751328001.125");
            add15Minutes.doClick();
            assertEquals(List.of("UTC: 2025-07-01T00:15:01.125Z", "Local: 2025-06-30T20:15:01.125-04:00"),
                    visiblePreviews(panel));

            valueField.setText("not a number");
            assertEquals(List.of(), visiblePreviews(panel));
            assertNotNull(panel.getValidationInfo());
            valueField.setText(NumericDate.fromInstant(Instant.MIN).toString());
            assertEquals(List.of("UTC: " + Instant.MIN), visiblePreviews(panel));
            assertNull(panel.getValidationInfo());
            valueField.setText(NumericDate.fromInstant(Instant.MAX).toString());
            assertEquals(List.of("UTC: " + Instant.MAX), visiblePreviews(panel));
        } finally {
            TimeZone.setDefault(originalZone);
        }
    }

    public void testTimestampEditorDoesNotDuplicateUtcPreview() {
        TimeZone originalZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
            DateClaimPanel panel = new DateClaimPanel(DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.1")));
            assertEquals(List.of("UTC: 2025-01-01T00:00:01.100Z"), visiblePreviews(panel));
        } finally {
            TimeZone.setDefault(originalZone);
        }
    }

    public void testLocalIsoTableUsesTheOffsetAtEachTimestampAndRetainsRawFallback() {
        TimeZone originalZone = TimeZone.getDefault();
        Configuration.TimestampFormat originalFormat = Configuration.INSTANCE.getTimestampFormat();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO_LOCAL);
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            jwt.setPayloadClaim(DateClaim.fromNumericDate("iat", new BigDecimal("1735689600")));
            jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", new BigDecimal("1751328001.125")));
            BigDecimal outsideLocalRange = BigDecimal.valueOf(Instant.MIN.getEpochSecond());
            jwt.setPayloadClaim(DateClaim.fromNumericDate("nbf", outsideLocalRange));
            JwtClaimsTableModel model = new JwtClaimsTableModel(jwt);

            assertEquals("2024-12-31T19:00:00-05:00", model.getValueAt(findRow(model, "iat"), 1));
            assertEquals("2025-06-30T20:00:01.125-04:00", model.getValueAt(findRow(model, "exp"), 1));
            assertEquals(outsideLocalRange.toString(), model.getValueAt(findRow(model, "nbf"), 1));
        } finally {
            Configuration.INSTANCE.setTimestampFormat(originalFormat);
            TimeZone.setDefault(originalZone);
        }
    }

    public void testTimestampTableShowsFullIsoDateTimeAndRetainsRawFallback() {
        Configuration.TimestampFormat originalFormat = Configuration.INSTANCE.getTimestampFormat();
        try {
            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO);
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            jwt.setPayloadClaim(DateClaim.fromNumericDate("iat", BigDecimal.ZERO));
            jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.125")));
            jwt.setPayloadClaim(DateClaim.fromNumericDate("nbf", new BigDecimal("-0.125")));
            JwtClaimsTableModel model = new JwtClaimsTableModel(jwt);

            assertEquals("1970-01-01T00:00:00Z", model.getValueAt(findRow(model, "iat"), 1));
            assertEquals("2025-01-01T00:00:01.125Z", model.getValueAt(findRow(model, "exp"), 1));
            assertEquals("1969-12-31T23:59:59.875Z", model.getValueAt(findRow(model, "nbf"), 1));

            BigDecimal outOfRange = new BigDecimal("999999999999999999999999");
            Jwt largeTimestampJwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            largeTimestampJwt.setPayloadClaim(DateClaim.fromNumericDate("exp", outOfRange));
            assertEquals(outOfRange.toString(), new JwtClaimsTableModel(largeTimestampJwt).getValueAt(0, 1));
        } finally {
            Configuration.INSTANCE.setTimestampFormat(originalFormat);
        }
    }

    public void testMediumTimestampUsesDefaultFormattingLocaleAndTimeZone() {
        Locale originalLocale = Locale.getDefault(Locale.Category.FORMAT);
        TimeZone originalZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("sk-SK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bratislava"));
            BigDecimal timestamp = NumericDate.fromInstant(Instant.parse("2026-11-22T17:50:00Z"));

            String slovak = TimestampFormatter.format(timestamp, Configuration.TimestampFormat.MEDIUM,
                    Instant.EPOCH);
            assertEquals("22. 11. 2026, 18:50:00", slovak);

            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            String english = TimestampFormatter.format(timestamp, Configuration.TimestampFormat.MEDIUM,
                    Instant.EPOCH);
            assertFalse(slovak.equals(english));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalLocale);
            TimeZone.setDefault(originalZone);
        }
    }

    public void testMediumTimestampRetainsRawFallbackWhenOutOfRange() {
        Locale originalLocale = Locale.getDefault(Locale.Category.FORMAT);
        TimeZone originalZone = TimeZone.getDefault();
        Configuration.TimestampFormat originalFormat = Configuration.INSTANCE.getTimestampFormat();
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("sk-SK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bratislava"));
            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.MEDIUM);

            BigDecimal outsideInstantRange = new BigDecimal("999999999999999999999999");
            assertNull(TimestampFormatter.format(outsideInstantRange,
                    Configuration.TimestampFormat.MEDIUM, Instant.EPOCH));
            assertNull(TimestampFormatter.format(NumericDate.fromInstant(Instant.MIN),
                    Configuration.TimestampFormat.MEDIUM, Instant.EPOCH));

            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", outsideInstantRange));
            JwtClaimsTableModel model = new JwtClaimsTableModel(jwt);
            assertEquals(outsideInstantRange.toString(), model.getValueAt(findRow(model, "exp"), 1));
        } finally {
            Configuration.INSTANCE.setTimestampFormat(originalFormat);
            Locale.setDefault(Locale.Category.FORMAT, originalLocale);
            TimeZone.setDefault(originalZone);
        }
    }

    public void testNumericDateConversionsHandleExtremeAndSubUnitValues() {
        BigDecimal outsideDisplayRange = new BigDecimal("1E+2147483647");
        assertNull(NumericDate.toInstant(outsideDisplayRange));
        assertNull(NumericDate.toDate(outsideDisplayRange));

        BigDecimal smallerThanNanosecond = new BigDecimal("-1E-2147483647");
        assertEquals(Instant.EPOCH, NumericDate.toInstant(smallerThanNanosecond));
        assertEquals(0L, NumericDate.toDate(smallerThanNanosecond).getTime());
    }

    private static int findRow(JwtClaimsTableModel model, String claimName) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (claimName.equals(model.getClaimAt(row).getName())) {
                return row;
            }
        }
        return -1;
    }

    private static List<String> visiblePreviews(DateClaimPanel panel) {
        return Arrays.stream(panel.getComponents())
                .filter(component -> component instanceof JLabel && component.isVisible())
                .map(component -> ((JLabel) component).getText())
                .toList();
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) {
                layoutRecursively(child);
            }
        }
    }

    private static JLabel findLabelWithText(Component root, String text) {
        if (root instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel result = findLabelWithText(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findComponentRecursively(Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T result = findComponentRecursively(child, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findComponent(DateClaimPanel panel, Class<T> type) {
        for (Component component : panel.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
        }
        return null;
    }

    private static JButton findButton(DateClaimPanel panel, String text) {
        return findButtonRecursively(panel, text);
    }

    private static JButton findButtonRecursively(Component root, String text) {
        if (root instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton result = findButtonRecursively(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
