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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        assertEquals("2025-01-01T00:00:01.125Z", datePreview.getText());
        assertEquals(0, claim.getValue().compareTo(panel.getClaimValue()));

        add15Minutes.doClick();
        assertEquals(0, new BigDecimal("1735690501.125").compareTo(panel.getClaimValue()));
        assertEquals("2025-01-01T00:15:01.125Z", datePreview.getText());
        assertNull(panel.getValidationInfo());

        valueField.setText("999999999999999999999999");
        assertEquals("Timestamp outside display range", datePreview.getText());

        valueField.setText("not a number");
        assertEquals("Not a valid timestamp", datePreview.getText());
        assertNotNull(panel.getValidationInfo());
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

    private static <T extends Component> T findComponent(DateClaimPanel panel, Class<T> type) {
        for (Component component : panel.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
        }
        return null;
    }

    private static JButton findButton(DateClaimPanel panel, String text) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
        }
        return null;
    }
}
