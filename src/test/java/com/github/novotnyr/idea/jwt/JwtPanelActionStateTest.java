package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.hs384.HS384SignatureContext;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;

public class JwtPanelActionStateTest extends LightPlatformTestCase {
    public void testClaimActionsFollowCurrentTokenSecretAndSelection() {
        JwtPanel panel = new JwtPanel(getProject());
        panel.getRootPanel().addNotify();
        try {
            AnActionButton add = ToolbarDecorator.findAddButton(panel.getRootPanel());
            AnActionButton edit = ToolbarDecorator.findEditButton(panel.getRootPanel());
            AnActionButton remove = ToolbarDecorator.findRemoveButton(panel.getRootPanel());
            JButton verifySignature = findVerifySignatureButton(panel.getVerificationToolbarComponent());
            JTable claims = findClaimsTable(panel.getRootPanel());
            assertNotNull(add);
            assertNotNull(edit);
            assertNotNull(remove);
            assertNotNull(verifySignature);
            assertNotNull(claims);
            assertTrue(claims.isShowing());
            assertEquals(ActionUpdateThread.EDT, add.getActionUpdateThread());
            assertEquals(ActionUpdateThread.EDT, edit.getActionUpdateThread());
            assertEquals(ActionUpdateThread.EDT, remove.getActionUpdateThread());
            assertFalse(isEnabled(add));
            assertFalse(isEnabled(edit));
            assertFalse(isEnabled(remove));
            assertFalse(verifySignature.isEnabled());

            Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            signed.setPayloadClaim(new StringClaim("subject", "alice"));
            panel.setJwt(new Jwt(signed.toString()));
            HS256Panel secretPanel = (HS256Panel) panel.getSecretPanel();
            assertFalse(isEnabled(add));
            assertFalse(verifySignature.isEnabled());

            secretPanel.setSecret("secret");
            assertTrue(isEnabled(add));
            assertFalse(isEnabled(edit));
            assertFalse(isEnabled(remove));
            assertTrue(verifySignature.isEnabled());

            claims.setRowSelectionInterval(0, 0);
            UIUtil.dispatchAllInvocationEvents();
            assertTrue(isEnabled(edit));
            assertTrue(isEnabled(remove));

            secretPanel.setSecret("");
            assertFalse(isEnabled(add));
            assertFalse(isEnabled(edit));
            assertFalse(isEnabled(remove));
            assertFalse(verifySignature.isEnabled());

            secretPanel.setSecret("secret");
            assertTrue(isEnabled(add));
            assertTrue(isEnabled(edit));
            assertTrue(isEnabled(remove));
            assertTrue(verifySignature.isEnabled());

            claims.clearSelection();
            UIUtil.dispatchAllInvocationEvents();
            assertTrue(isEnabled(add));
            assertFalse(isEnabled(edit));
            assertFalse(isEnabled(remove));

            Jwt hs384 = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS384,
                    new HS384SignatureContext("other secret"), false);
            panel.setJwt(new Jwt(hs384.toString()));
            assertFalse(isEnabled(add));
            assertFalse(verifySignature.isEnabled());

            panel.reset();
            assertFalse(isEnabled(add));
            assertFalse(isEnabled(edit));
            assertFalse(isEnabled(remove));
            assertFalse(verifySignature.isEnabled());
        } finally {
            panel.getRootPanel().removeNotify();
            Disposer.dispose(panel);
        }
    }

    public void testReadOnlyClaimDialogExplainsWhyClaimCannotBeEdited() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            Jwt hs256 = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            StringClaim claim = new StringClaim("subject", "alice");
            hs256.setPayloadClaim(claim);
            panel.setJwt(new Jwt(hs256.toString()));

            String sharedSecretReason = panel.getClaimViewOnlyReason();
            assertEquals("Read-only: Enter a shared secret in Key / Secret to edit claims.", sharedSecretReason);
            ClaimDialog dialog = new ClaimDialog(claim, ClaimDialog.Mode.VIEW, sharedSecretReason);
            JLabel note = findComponentNamed((Component) dialog.createCenterPanel(),
                    "jwt.claim.readOnly.note", JLabel.class);
            assertNotNull(note);
            assertEquals(sharedSecretReason, note.getText());

            panel.setJwt(new Jwt("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.c2ln"));
            assertEquals("Read-only: Enter the RSA public and private keys in Key / Secret to edit claims.",
                    panel.getClaimViewOnlyReason());

            panel.setJwt(new Jwt("eyJhbGciOiJ1bmtub3duIn0.eyJzdWIiOiJhbGljZSJ9.c2ln"));
            assertEquals("Read-only: The token's algorithm is not supported.", panel.getClaimViewOnlyReason());
        } finally {
            Disposer.dispose(panel);
        }
    }

    private static boolean isEnabled(AnActionButton button) {
        AnActionEvent event = TestActionEvent.createTestEvent(button);
        button.update(event);
        return event.getPresentation().isEnabled();
    }

    private static JButton findVerifySignatureButton(Component root) {
        if (root instanceof JButton button && "Verify Signature".equals(button.getText())) {
            return button;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton result = findVerifySignatureButton(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static JTable findClaimsTable(Component root) {
        if (root instanceof JTable table && Constants.CLAIMS_TABLE_NAME.equals(table.getName())) {
            return table;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable result = findClaimsTable(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findComponentNamed(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T result = findComponentNamed(child, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
