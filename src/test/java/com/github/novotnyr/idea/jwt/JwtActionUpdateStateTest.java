package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.action.CreateScratchFileAction;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.util.ui.UIUtil;

public class JwtActionUpdateStateTest extends LightPlatformTestCase {
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
            encoded.setJwt(tokenWithClaim());
            assertTrue(isEnabled(copy));
            assertTrue(isEnabled(scratch));

            encoded.reset();
            assertFalse(isEnabled(copy));
            assertFalse(isEnabled(scratch));
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
            AnAction iso = findAction(formats, "ISO");
            AnAction relative = findAction(formats, "Relative");
            for (AnAction action : formats.getChildren(null)) {
                assertEquals(ActionUpdateThread.EDT, action.getActionUpdateThread());
            }

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RAW);
            assertSelected(raw, iso, relative);

            iso.actionPerformed(TestActionEvent.createTestEvent(iso));
            assertSelected(iso, raw, relative);

            relative.actionPerformed(TestActionEvent.createTestEvent(relative));
            assertSelected(relative, raw, iso);
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

    private static void assertSelected(AnAction selected, AnAction other1, AnAction other2) {
        assertTrue(isSelected(selected));
        assertFalse(isSelected(other1));
        assertFalse(isSelected(other2));
    }
}
