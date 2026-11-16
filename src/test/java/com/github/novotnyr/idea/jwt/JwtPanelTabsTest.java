package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;

public class JwtPanelTabsTest extends LightPlatformTestCase {
    public void testPayloadAndHeaderTabsShareSigningControls() {
        JwtPanel panel = new JwtPanel(getProject());
        panel.getRootPanel().addNotify();
        try {
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            assertEquals(2, tabs.getTabCount());
            assertEquals("Payload", tabs.getTitleAt(0));
            assertEquals("Header", tabs.getTitleAt(1));
            assertEquals(0, tabs.getSelectedIndex());
            Component footer = ((BorderLayout) panel.getRootPanel().getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            assertTrue(footer instanceof JButton);
            assertEquals("Validate", ((JButton) footer).getText());

            String fullValue = "header-value-".repeat(30);
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            jwt.setHeaderClaim(new StringClaim("custom", fullValue));
            jwt.rebuild();
            panel.setJwt(new Jwt(jwt.toString()));

            JBTable header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            assertNotNull(header);
            assertEquals("Claim", header.getColumnName(0));
            assertEquals("Value", header.getColumnName(1));
            assertEquals(3, header.getRowCount());
            assertEquals("custom", header.getValueAt(2, 0));
            assertEquals(fullValue, header.getValueAt(2, 1));
            assertNotNull(header.getComponentPopupMenu());
            assertFalse(header.isCellEditable(2, 1));

            header.setRowSelectionInterval(2, 2);
            panel.copySelectedHeaderValue();
            assertEquals(fullValue,
                    CopyPasteManagerEx.getInstanceEx().getContents(DataFlavor.stringFlavor));

            HS256Panel secret = (HS256Panel) panel.getSecretPanel();
            secret.setSecret("secret");
            assertTrue(((JButton) footer).isEnabled());
            tabs.setSelectedIndex(1);
            assertTrue(SwingUtilities.isDescendingFrom(secret.getRoot(), panel.getRootPanel()));
            assertFalse(SwingUtilities.isDescendingFrom(secret.getRoot(), tabs));
            assertTrue(secret.getRoot().isShowing());
            assertTrue(footer.isShowing());

            panel.setJwt(new Jwt(jwt.toString()));
            assertEquals(0, tabs.getSelectedIndex());
        } finally {
            panel.getRootPanel().removeNotify();
            Disposer.dispose(panel);
        }
    }

    public void testClearingParsedTokenRetainsSigningInput() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            jwt.setPayloadClaim(new StringClaim("subject", "alice"));
            panel.setJwt(new Jwt(jwt.toString()));
            HS256Panel secret = (HS256Panel) panel.getSecretPanel();
            secret.setSecret("secret");
            JButton validate = (JButton) ((BorderLayout) panel.getRootPanel().getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            assertTrue(validate.isEnabled());

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            tabs.setSelectedIndex(1);
            panel.clearParsedJwtKeepingSigningInput();

            JBTable header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertNotNull(header);
            assertNotNull(claims);
            assertEquals(0, header.getRowCount());
            assertEquals(0, claims.getRowCount());
            assertSame(secret, panel.getSecretPanel());
            assertEquals("secret", ((HS256SignatureContext) secret.getSignatureContext()).getSecret());
            assertFalse(validate.isEnabled());

            panel.reset();
            assertEquals(0, tabs.getSelectedIndex());
        } finally {
            Disposer.dispose(panel);
        }
    }
}
