package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.ui.claimpanel.DateClaimPanel;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.github.novotnyr.idea.jwt.validation.JwtValidator;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.testFramework.LightPlatformTestCase;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
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

    public void testTimestampErrorHighlightsOnlyExactClaimName() {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setPayloadClaim(new StringClaim("iat", "yes"));
        jwt.setPayloadClaim(new StringClaim("IAT", "custom value"));
        JwtClaimsTableModel model = new JwtClaimsTableModel(jwt);
        JTable table = new JTable(model);
        Color errorColor = EditorColorsManager.getInstance().getGlobalScheme()
                .getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES).getEffectColor();
        Color normalColor = Color.BLACK.equals(errorColor) ? Color.WHITE : Color.BLACK;
        table.setForeground(normalColor);

        model.setClaimErrors(new JwtValidator().validateClaims(jwt).claimErrors());

        assertEquals(errorColor, table.prepareRenderer(model, 0, 0).getForeground());
        assertEquals(normalColor, table.prepareRenderer(model, 1, 0).getForeground());
    }

    public void testTimestampEditorAcceptsFractionalSeconds() {
        DateClaim claim = DateClaim.fromNumericDate("exp", new BigDecimal("1735689601.125"));
        DateClaimPanel panel = new DateClaimPanel(claim);
        JTextField valueField = findComponent(panel, JTextField.class);
        JButton add15Minutes = findButton(panel, "+15 min");
        assertNotNull(valueField);
        assertNotNull(add15Minutes);
        assertEquals("1735689601.125", valueField.getText());
        assertNull(panel.getValidationInfo());
        assertEquals(0, claim.getValue().compareTo(panel.getClaimValue()));

        add15Minutes.doClick();
        assertEquals(0, new BigDecimal("1735690501.125").compareTo(panel.getClaimValue()));
        assertNull(panel.getValidationInfo());

        valueField.setText("not a number");
        assertNotNull(panel.getValidationInfo());
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
