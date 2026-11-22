package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.action.CreateScratchFileAction;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

public class JwtActionUpdateStateTest extends LightPlatformTestCase {
    public void testVerifySignatureStaysVisibleAtTrailingEdgeOfToolbar() {
        JwtExplorer explorer = new JwtExplorer(getProject());
        explorer.addNotify();
        try {
            JPanel toolbar = findComponentNamed(explorer, "jwt.toolbar", JPanel.class);
            JPanel verificationControls = findComponentNamed(explorer, "jwt.verification.controls", JPanel.class);
            JButton verifySignature = findComponentNamed(explorer, "jwt.verifySignature.button", JButton.class);
            assertNotNull(toolbar);
            assertNotNull(verificationControls);
            assertNotNull(verifySignature);
            assertNull(findComponentNamed(toolbar, "jwt.signature.status", JComponent.class));
            toolbar.setSize(360, toolbar.getPreferredSize().height);
            layoutRecursively(toolbar);
            assertTrue("Verify Signature should remain a full-width primary action",
                    verifySignature.getWidth() >= verifySignature.getPreferredSize().width);
            assertEquals(verificationControls.getWidth(), verifySignature.getX() + verifySignature.getWidth());
            assertEquals(Boolean.TRUE, verifySignature.getClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY));

            JTabbedPane tabs = UIUtil.findComponentOfType(explorer, JTabbedPane.class);
            assertNotNull(tabs);
            for (int tabIndex = 0; tabIndex < tabs.getTabCount(); tabIndex++) {
                tabs.setSelectedIndex(tabIndex);
                assertTrue("Verify Signature should remain visible on every tab", verifySignature.isVisible());
            }

            toolbar.setSize(280, toolbar.getPreferredSize().height);
            layoutRecursively(toolbar);

            assertTrue("The verification action must not be clipped when the toolbar is narrow",
                    verifySignature.getWidth() >= verifySignature.getPreferredSize().width);
            assertEquals(verificationControls.getWidth(), verifySignature.getX() + verifySignature.getWidth());
        } finally {
            explorer.removeNotify();
            Disposer.dispose(explorer);
        }
    }

    public void testExplorerActionsFollowCurrentToken() {
        JwtExplorer explorer = new JwtExplorer(getProject());
        try {
            ActionGroup toolbar = (ActionGroup) explorer.getActions(false).getFirst();
            AnAction copy = findAction(toolbar, "Copy payload as JSON");
            AnAction scratch = findAction(toolbar, "Create scratch file from payload");
            assertTrue(scratch instanceof CreateScratchFileAction);
            assertEquals(ActionUpdateThread.EDT, copy.getActionUpdateThread());
            assertEquals(ActionUpdateThread.EDT, scratch.getActionUpdateThread());
            assertFalse(isEnabled(copy));
            assertFalse(isEnabled(scratch));

            EncodedJwtPanel encoded = UIUtil.findComponentOfType(explorer, EncodedJwtPanel.class);
            assertNotNull(encoded);
            JPanel missingInputHint = findComponentNamed(explorer, "jwt.verification.input.hint", JPanel.class);
            JLabel missingInputMessage = findComponentNamed(explorer,
                    "jwt.verification.input.message", JLabel.class);
            assertNotNull(missingInputHint);
            assertNotNull(missingInputMessage);
            JBTextArea input = UIUtil.findComponentOfType(encoded, JBTextArea.class);
            assertNotNull(input);
            input.setText(tokenWithClaim().toString());
            assertTrue(isEnabled(copy));
            assertTrue(isEnabled(scratch));
            assertTrue(missingInputHint.isVisible());
            assertEquals("Enter a shared secret in Key / Secret", missingInputMessage.getText());
            assertEquals(missingInputMessage.getText(), missingInputMessage.getToolTipText());

            JTabbedPane tabs = UIUtil.findComponentOfType(explorer, JTabbedPane.class);
            assertNotNull(tabs);
            assertEquals(3, tabs.getTabCount());
            assertEquals("Payload", tabs.getTitleAt(0));
            assertEquals("Header", tabs.getTitleAt(1));
            assertEquals("Key / Secret", tabs.getTitleAt(2));
            assertEquals(0, tabs.getSelectedIndex());
            for (int tabIndex = 0; tabIndex < tabs.getTabCount(); tabIndex++) {
                tabs.setSelectedIndex(tabIndex);
                assertTrue("Missing-input guidance should remain visible on every tab", missingInputHint.isVisible());
            }
            JTextField secretField = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(2), JTextField.class);
            assertNotNull(secretField);
            tabs.setSelectedIndex(2);
            secretField.setText("draft secret");
            assertFalse(missingInputHint.isVisible());

            input.setText("invalid.jwt");
            assertFalse(isEnabled(copy));
            assertFalse(isEnabled(scratch));
            assertFalse(missingInputHint.isVisible());
            assertEquals(2, tabs.getSelectedIndex());
            assertEquals("draft secret", secretField.getText());

            JBTable header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertNotNull(header);
            assertNotNull(claims);
            assertEquals(0, header.getRowCount());
            assertEquals(0, claims.getRowCount());

            encoded.reset();
            assertFalse(isEnabled(copy));
            assertFalse(isEnabled(scratch));
            assertFalse(missingInputHint.isVisible());
            assertEquals(2, tabs.getSelectedIndex());
            assertEquals("draft secret", secretField.getText());

            AnAction reset = findAction(toolbar, "Reset");
            reset.actionPerformed(TestActionEvent.createTestEvent(reset));
            assertEquals(0, tabs.getSelectedIndex());
            assertFalse(missingInputHint.isVisible());
            assertFalse(SwingUtilities.isDescendingFrom(secretField, tabs.getComponentAt(2)));
        } finally {
            Disposer.dispose(explorer);
        }
    }

    public void testClaimsCopyActionFollowsPayload() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            CommonActionsPanel actions = UIUtil.findComponentOfType(panel.getRootPanel(), CommonActionsPanel.class);
            assertNotNull(actions);
            AnAction copy = findAction(actions.getToolbar().getActionGroup(), "Copy payload as JSON");
            assertEquals(ActionUpdateThread.EDT, copy.getActionUpdateThread());
            assertFalse(isEnabled(copy));

            panel.setJwt(JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false));
            assertFalse(isEnabled(copy));

            panel.setJwt(tokenWithClaim());
            assertTrue(isEnabled(copy));

            panel.reset();
            assertFalse(isEnabled(copy));
        } finally {
            Disposer.dispose(panel);
        }
    }

    @SuppressWarnings("OverrideOnly")
    public void testTimestampMenuFollowsCurrentFormat() {
        Configuration.TimestampFormat original = Configuration.INSTANCE.getTimestampFormat();
        try {
            ActionGroup outer = new ToolWindowFactory().getToolWindowActionGroup();
            ActionGroup formats = (ActionGroup) outer.getChildren(null)[0];
            AnAction raw = findAction(formats, "Unix timestamp");
            AnAction iso = findAction(formats, "ISO (UTC)");
            AnAction localIso = findAction(formats, "ISO (Local)");
            AnAction relative = findAction(formats, "Relative");
            AnAction medium = findAction(formats, "Medium Format");
            assertEquals(5, formats.getChildren(null).length);
            assertEquals("Medium Format",
                    formats.getChildren(null)[3].getTemplatePresentation().getText());
            for (AnAction action : formats.getChildren(null)) {
                assertEquals(ActionUpdateThread.EDT, action.getActionUpdateThread());
            }

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RAW);
            assertSelected(raw, iso, localIso, relative, medium);

            iso.actionPerformed(TestActionEvent.createTestEvent(iso));
            assertSelected(iso, raw, localIso, relative, medium);

            localIso.actionPerformed(TestActionEvent.createTestEvent(localIso));
            assertSelected(localIso, raw, iso, relative, medium);
            localIso.actionPerformed(TestActionEvent.createTestEvent(localIso));
            assertSelected(localIso, raw, iso, relative, medium);

            relative.actionPerformed(TestActionEvent.createTestEvent(relative));
            assertSelected(relative, raw, iso, localIso, medium);

            medium.actionPerformed(TestActionEvent.createTestEvent(medium));
            assertSelected(medium, raw, iso, localIso, relative);
            medium.actionPerformed(TestActionEvent.createTestEvent(medium));
            assertSelected(medium, raw, iso, localIso, relative);
        } finally {
            Configuration.INSTANCE.setTimestampFormat(original);
        }
    }

    private static Jwt tokenWithClaim() {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setPayloadClaim(new StringClaim("subject", "alice"));
        return jwt;
    }

    @SuppressWarnings("OverrideOnly")
    private static AnAction findAction(ActionGroup group, String text) {
        for (AnAction action : group.getChildren(null)) {
            if (text.equals(action.getTemplatePresentation().getText())) {
                return action;
            }
        }
        throw new AssertionError("Action not found: " + text);
    }

    @SuppressWarnings("OverrideOnly")
    private static boolean isEnabled(AnAction action) {
        AnActionEvent event = TestActionEvent.createTestEvent(action);
        action.update(event);
        return event.getPresentation().isEnabled();
    }

    @SuppressWarnings("OverrideOnly")
    private static boolean isSelected(AnAction action) {
        AnActionEvent event = TestActionEvent.createTestEvent(action);
        action.update(event);
        return Toggleable.isSelected(event.getPresentation());
    }

    private static void assertSelected(AnAction selected, AnAction... others) {
        assertTrue(isSelected(selected));
        for (AnAction other : others) {
            assertFalse(isSelected(other));
        }
    }

    private static <T extends JComponent> T findComponentNamed(Component root, String name, Class<T> type) {
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

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                layoutRecursively(childContainer);
            }
        }
    }
}
