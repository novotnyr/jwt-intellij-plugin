package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

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

    public void testExpiredTokenShowsWarningWithoutValidatingSignature() {
        Instant expiration = Instant.now().minusSeconds(10);
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", NumericDate.fromInstant(expiration)));
        jwt.rebuild();

        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt(jwt.toString()));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertNotNull("The expired token should show a warning before signature validation", warning);
            assertTrue(warning.isVisible());
            JLabel message = findComponentNamed(warning, "jwt.expiration.message", JLabel.class);
            assertNotNull(message);
            assertEquals("Token expired at " + expiration, message.getText());

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            int expirationRow = findRow(claims, "exp");
            assertTrue(expirationRow >= 0);
            assertTrue(hasErrorIcon(claims, expirationRow, 1));

            tabs.setSelectedIndex(1);
            assertTrue(warning.isVisible());
            panel.setSignatureContext(new HS256SignatureContext("secret"));
            JButton validate = (JButton) ((BorderLayout) panel.getRootPanel().getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            validate.doClick();
            assertTrue(warning.isVisible());
            assertTrue(hasErrorIcon(claims, expirationRow, 1));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testFutureExpirationIsRefreshedWithoutTokenInputOrSignatureValidation() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            Instant expiration = now.plusMillis(2_000).plusNanos(500_000);
            panel.setJwt(new Jwt(createJwtWithExpiration(NumericDate.fromInstant(expiration)).toString()));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertNotNull(warning);
            assertFalse(warning.isVisible());
            assertEquals(2_001, scheduler.delayMillis);

            clock.setInstant(expiration);
            scheduler.runScheduledTask();

            assertTrue(warning.isVisible());
            JLabel message = findComponentNamed(warning, "jwt.expiration.message", JLabel.class);
            assertEquals("Token expired at " + expiration, message.getText());
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertTrue(hasErrorIcon(claims, findRow(claims, "exp"), 1));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testExpirationWarningClearsForMissingMalformedAndEditedClaims() {
        Instant expiration = Instant.now().minusSeconds(10);
        JwtPanel panel = new JwtPanel(getProject());
        try {
            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertNotNull(warning);

            panel.setJwt(new Jwt(createJwtWithExpiration(NumericDate.fromInstant(expiration)).toString()));
            assertTrue(warning.isVisible());

            panel.setJwt(new Jwt(JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false).toString()));
            assertFalse(warning.isVisible());

            Jwt malformedExpiration = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            malformedExpiration.setPayloadClaim(new StringClaim("exp", "not a number"));
            malformedExpiration.rebuild();
            panel.setJwt(new Jwt(malformedExpiration.toString()));
            assertFalse(warning.isVisible());

            Jwt issuedAtOnly = createJwtWithIssuedAt(BigDecimal.valueOf(1_700_000_000L));
            panel.setJwt(new Jwt(issuedAtOnly.toString()));
            assertFalse(warning.isVisible());

            panel.setJwt(new Jwt(createJwtWithExpiration(NumericDate.fromInstant(expiration)).toString()));
            assertTrue(warning.isVisible());
            panel.setSignatureContext(new HS256SignatureContext("secret"));
            panel.updateClaim(null, "exp");
            assertFalse(warning.isVisible());

            panel.setJwt(new Jwt(createJwtWithExpiration(NumericDate.fromInstant(expiration)).toString()));
            assertTrue(warning.isVisible());
            panel.clearParsedJwtKeepingSigningInput();
            assertFalse(warning.isVisible());
        } finally {
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
            assertEquals("secret", ((HS256SignatureContext) secret.getSignatureContext()).secret());
            assertFalse(validate.isEnabled());

            panel.reset();
            assertEquals(0, tabs.getSelectedIndex());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testEditingIssuedAtToStringKeepsClaimVisible() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            HS256SignatureContext context = new HS256SignatureContext("secret");
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256, context, true);
            panel.setJwt(jwt);
            panel.setSignatureContext(context);

            panel.updateClaim(null, "iat");
            panel.updateClaim(new StringClaim("iat", "yes"), null);

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertNotNull(claims);
            assertEquals(1, claims.getRowCount());
            assertEquals("iat", claims.getValueAt(0, 0));
            assertEquals("yes", claims.getValueAt(0, 1));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testClaimErrorIconIsLimitedToInvalidPayloadValueAndRefreshes() {
        Configuration.TimestampFormat originalFormat = Configuration.INSTANCE.getTimestampFormat();
        Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO);
        JwtPanel panel = new JwtPanel(getProject());
        try {
            Jwt invalidJwt = createJwtWithIssuedAt("yes");
            panel.setJwt(new Jwt(invalidJwt.toString()));

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            JBTable header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            assertNotNull(claims);
            assertNotNull(header);

            int issuedAtRow = findRow(claims, "iat");
            int subjectRow = findRow(claims, "subject");
            assertTrue(issuedAtRow >= 0);
            assertTrue(subjectRow >= 0);
            assertEquals("yes", claims.getValueAt(issuedAtRow, 1));
            assertTrue(hasErrorIcon(claims, issuedAtRow, 1));
            assertFalse(hasErrorIcon(claims, issuedAtRow, 0));
            assertFalse(hasErrorIcon(claims, subjectRow, 1));
            assertEquals("header-value", header.getValueAt(findRow(header, "custom"), 1));

            JwtClaimsTableModel model = (JwtClaimsTableModel) claims.getModel();
            model.setClaimErrors(java.util.List.of());
            assertFalse(hasErrorIcon(claims, issuedAtRow, 1));

            panel.setSignatureContext(new HS256SignatureContext("secret"));
            JButton validate = (JButton) ((BorderLayout) panel.getRootPanel().getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            assertTrue(validate.isEnabled());
            validate.doClick();
            assertTrue(hasErrorIcon(claims, issuedAtRow, 1));

            panel.updateClaim(DateClaim.fromNumericDate("iat", BigDecimal.valueOf(1_700_000_000L)), null);
            claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            issuedAtRow = findRow(claims, "iat");
            assertFalse(hasErrorIcon(claims, issuedAtRow, 1));
            assertEquals("2023-11-14T22:13:20Z", claims.getValueAt(issuedAtRow, 1));

            panel.setJwt(new Jwt(invalidJwt.toString()));
            claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            issuedAtRow = findRow(claims, "iat");
            assertTrue(hasErrorIcon(claims, issuedAtRow, 1));

            Jwt validJwt = createJwtWithIssuedAt(BigDecimal.valueOf(1_700_000_000L));
            panel.setJwt(new Jwt(validJwt.toString()));
            claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            issuedAtRow = findRow(claims, "iat");
            assertFalse(hasErrorIcon(claims, issuedAtRow, 1));
            header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            assertEquals("header-value", header.getValueAt(findRow(header, "custom"), 1));
        } finally {
            Disposer.dispose(panel);
            Configuration.INSTANCE.setTimestampFormat(originalFormat);
        }
    }

    public void testClaimErrorTooltipResolutionUsesStoredMessageAndClearsAfterReplacement() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt(createJwtWithIssuedAt("yes").toString()));

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertNotNull(claims);

            int issuedAtRow = findRow(claims, "iat");
            int subjectRow = findRow(claims, "subject");
            JwtClaimsTableModel model = (JwtClaimsTableModel) claims.getModel();
            ClaimError storedError = model.getClaimErrorAt(claims.convertRowIndexToModel(issuedAtRow));
            assertNotNull(storedError);

            Rectangle invalidCell = claims.getCellRect(issuedAtRow, 1, true);
            ClaimError hoverError = panel.getClaimErrorAtPoint(new Point(
                    invalidCell.x + 1, invalidCell.y + 1));
            claims.setRowSelectionInterval(issuedAtRow, issuedAtRow);
            ClaimError focusedRowError = panel.getClaimErrorAtFocusedRow();
            assertSame(storedError, hoverError);
            assertSame(storedError, focusedRowError);
            assertEquals("Illegal type: NumericDate must be a JSON number", hoverError.getMessage());
            assertNull(panel.getClaimErrorAtViewCell(subjectRow, 1));
            assertNull(panel.getClaimErrorAtViewCell(issuedAtRow, 0));
            assertNull(panel.getClaimErrorAtPoint(new Point(
                    claims.getCellRect(subjectRow, 1, true).x + 1,
                    claims.getCellRect(subjectRow, 1, true).y + 1)));

            panel.setJwt(new Jwt(createJwtWithIssuedAt(BigDecimal.valueOf(1_700_000_000L)).toString()));
            claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            issuedAtRow = findRow(claims, "iat");
            assertNull(panel.getClaimErrorAtFocusedRow());
            assertNull(panel.getClaimErrorAtViewCell(issuedAtRow, 1));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testClaimColumnsStartNarrowAndKeepUserResizing() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            JBTable header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            assertNotNull(claims);
            assertNotNull(header);

            assertClaimColumnNearThirtyPercent(claims, 350);
            assertClaimColumnNearThirtyPercent(header, 700);

            claims.getColumnModel().getColumn(0).setPreferredWidth(190);
            header.getColumnModel().getColumn(0).setPreferredWidth(180);
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            panel.setJwt(new Jwt(jwt.toString()));
            assertEquals(190, claims.getColumnModel().getColumn(0).getPreferredWidth());
            assertEquals(180, header.getColumnModel().getColumn(0).getPreferredWidth());
            assertTrue(claims.getTableHeader().getResizingAllowed());
            assertTrue(header.getTableHeader().getResizingAllowed());
        } finally {
            Disposer.dispose(panel);
        }
    }

    private static void assertClaimColumnNearThirtyPercent(JBTable table, int tableWidth) {
        table.setSize(tableWidth, 100);
        table.doLayout();
        int claimWidth = table.getColumnModel().getColumn(0).getWidth();
        float claimShare = (float) claimWidth / tableWidth;
        assertTrue("Claim share at width " + tableWidth + " was " + claimShare,
                claimShare >= 0.25f && claimShare <= 0.35f);
    }

    private static Jwt createJwtWithIssuedAt(Object issuedAt) {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setHeaderClaim(new StringClaim("custom", "header-value"));
        jwt.setPayloadClaim(new StringClaim("subject", "alice"));
        if (issuedAt instanceof String value) {
            jwt.setPayloadClaim(new StringClaim("iat", value));
        } else {
            jwt.setPayloadClaim(DateClaim.fromNumericDate("iat", (BigDecimal) issuedAt));
        }
        jwt.rebuild();
        return jwt;
    }

    private static Jwt createJwtWithExpiration(BigDecimal expiration) {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", expiration));
        jwt.rebuild();
        return jwt;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(this.instant, zone);
        }

        @Override
        public Instant instant() {
            return this.instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }
    }

    private static final class ManualExpirationScheduler implements JwtPanel.ExpirationRefreshScheduler {
        private long delayMillis;
        private Runnable scheduledTask;

        @Override
        public void schedule(long delayMillis, Runnable task) {
            this.delayMillis = delayMillis;
            this.scheduledTask = task;
        }

        @Override
        public void cancel() {
            this.scheduledTask = null;
        }

        private void runScheduledTask() {
            Runnable task = this.scheduledTask;
            this.scheduledTask = null;
            if (task == null) {
                throw new AssertionError("No expiration refresh was scheduled");
            }
            task.run();
        }
    }

    private static int findRow(JBTable table, String claimName) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (claimName.equals(table.getValueAt(row, 0))) {
                return row;
            }
        }
        return -1;
    }

    private static <T extends JComponent> T findComponentNamed(Container container, String name, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent jComponent && name.equals(jComponent.getName())
                    && type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T nested = findComponentNamed(child, name, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean hasErrorIcon(JBTable table, int row, int column) {
        if (row < 0) {
            return false;
        }
        Component renderer = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
        return containsIcon(renderer, AllIcons.General.BalloonError);
    }

    private static boolean containsIcon(Component component, Icon icon) {
        if (component instanceof JLabel label && icon.equals(label.getIcon())) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (containsIcon(child, icon)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void testUnsupportedAlgorithmIsNamedInSharedControls() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(tokenWithAlgorithm("HS512"));
            assertTrue(panel.getSecretPanel() instanceof UnrecognizedSecretPanel);
            JLabel status = UIUtil.findComponentOfType(panel.getSecretPanel().getRoot(), JLabel.class);
            assertNotNull(status);
            assertEquals("Unsupported algorithm: HS512", status.getText());

            panel.setJwt(tokenWithAlgorithm("PS256"));
            assertEquals("Unsupported algorithm: PS256", status.getText());

            panel.clearParsedJwtKeepingSigningInput();
            assertEquals("No valid JWT to validate", status.getText());
        } finally {
            Disposer.dispose(panel);
        }
    }

    private static Jwt tokenWithAlgorithm(String algorithm) {
        String headerJson = "{\"alg\":\"" + algorithm + "\",\"typ\":\"JWT\"}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        return new Jwt(header + "." + payload + ".c2ln");
    }
}
