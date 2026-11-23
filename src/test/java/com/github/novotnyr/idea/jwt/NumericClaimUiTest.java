package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.ui.claimpanel.NumericClaimPanel;
import com.intellij.testFramework.LightPlatformTestCase;

import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;

public class NumericClaimUiTest extends LightPlatformTestCase {
    public void testAddAndEditDialogsAcceptAndRoundTripFractionalNumericClaims() {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);

        TestClaimDialog addDialog = new TestClaimDialog(new NumericClaim("claim", 0L), ClaimDialog.Mode.NEW);
        NumericClaimPanel addPanel = findComponent((Component) addDialog.createCenterPanel(), NumericClaimPanel.class);
        JTextField addValue = (JTextField) addPanel.getComponent(0);
        addValue.setText("0,2");
        assertNull(addPanel.getValidationInfo());

        addDialog.commit();
        NumericClaim addedClaim = (NumericClaim) addDialog.getClaim();
        jwt.setPayloadClaim(addedClaim);
        assertNumericClaim(new Jwt(jwt.toString()), "claim", "0.2");

        TestClaimDialog editDialog = new TestClaimDialog(new NumericClaim("claim", 1L), ClaimDialog.Mode.EDIT);
        NumericClaimPanel editPanel = findComponent((Component) editDialog.createCenterPanel(), NumericClaimPanel.class);
        JTextField editValue = (JTextField) editPanel.getComponent(0);
        editValue.setText("0.35");
        assertNull(editPanel.getValidationInfo());

        editDialog.commit();
        NumericClaim editedClaim = (NumericClaim) editDialog.getClaim();
        jwt.setPayloadClaim(editedClaim);
        assertNumericClaim(new Jwt(jwt.toString()), "claim", "0.35");
    }

    public void testNumericClaimDialogRejectsMalformedDecimalInput() {
        TestClaimDialog dialog = new TestClaimDialog(new NumericClaim("claim", 0L), ClaimDialog.Mode.NEW);
        NumericClaimPanel panel = findComponent((Component) dialog.createCenterPanel(), NumericClaimPanel.class);
        JTextField valueField = (JTextField) panel.getComponent(0);

        valueField.setText("0,2.3");

        assertNotNull(panel.getValidationInfo());
    }

    private static void assertNumericClaim(Jwt jwt, String claimName, String expected) {
        NamedClaim<?> claim = jwt.getPayloadClaims().stream()
                .filter(candidate -> claimName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertTrue(claim instanceof NumericClaim);
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) claim.getValue()));
    }

    private static <T extends Component> T findComponent(Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T result = findComponent(child, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static class TestClaimDialog extends ClaimDialog {
        private TestClaimDialog(NumericClaim claim, Mode mode) {
            super(claim, mode, null);
        }

        private void commit() {
            doOKAction();
        }
    }
}