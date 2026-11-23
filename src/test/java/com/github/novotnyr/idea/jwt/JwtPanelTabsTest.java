package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.hs384.HS384Panel;
import com.github.novotnyr.idea.jwt.hs384.HS384SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
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
import java.util.Locale;
import java.util.TimeZone;

public class JwtPanelTabsTest extends LightPlatformTestCase {
    public void testPayloadHeaderAndKeySecretTabsShareSigningControls() {
        JwtPanel panel = new JwtPanel(getProject());
        panel.getRootPanel().addNotify();
        try {
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            assertEquals(3, tabs.getTabCount());
            assertEquals("Payload", tabs.getTitleAt(0));
            assertEquals("Header", tabs.getTitleAt(1));
            assertEquals("Signature", tabs.getTitleAt(2));
            assertEquals(0, tabs.getSelectedIndex());
            Component footer = ((BorderLayout) panel.getRootPanel().getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            assertNull(footer);
            JPanel feedbackStack = findComponentNamed(panel.getRootPanel(), "jwt.feedback.stack", JPanel.class);
            JPanel signatureResult = findComponentNamed(panel.getRootPanel(), "jwt.signature.result", JPanel.class);
            assertNotNull(feedbackStack);
            assertNotNull(signatureResult);
            JPanel parsedContent = (JPanel) tabs.getParent();
            BorderLayout parsedLayout = (BorderLayout) parsedContent.getLayout();
            assertSame(feedbackStack, parsedLayout.getLayoutComponent(BorderLayout.NORTH));
            assertSame(tabs, parsedLayout.getLayoutComponent(BorderLayout.CENTER));
            assertSame(parsedContent, ((BorderLayout) panel.getRootPanel().getLayout())
                    .getLayoutComponent(BorderLayout.CENTER));
            assertFalse(feedbackStack.isVisible());
            assertFalse(signatureResult.isVisible());

            String fullValue = "header-value-".repeat(30);
            Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                    new HS256SignatureContext("secret"), false);
            jwt.setHeaderClaim(new StringClaim("custom", fullValue));
            jwt.rebuild();
            panel.setJwt(new Jwt(jwt.toString()));
            assertEquals("HS256", tabs.getTitleAt(2));

            JBTable payload = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            JBTable header = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(1), JBTable.class);
            assertNotNull(payload);
            assertNotNull(header);
            assertTableHasNoInnerBorders(payload);
            assertTableHasNoInnerBorders(header);
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
            tabs.setSelectedIndex(1);
            assertTrue(SwingUtilities.isDescendingFrom(secret.getRoot(), panel.getRootPanel()));
            assertTrue(SwingUtilities.isDescendingFrom(secret.getRoot(), tabs));
            assertFalse(secret.getRoot().isShowing());
            tabs.setSelectedIndex(2);
            assertTrue(secret.getRoot().isShowing());
            tabs.setSelectedIndex(0);
            assertFalse(secret.getRoot().isShowing());

            panel.setJwt(new Jwt(jwt.toString()));
            assertEquals(0, tabs.getSelectedIndex());
        } finally {
            panel.getRootPanel().removeNotify();
            Disposer.dispose(panel);
        }
    }

    private static void assertTableHasNoInnerBorders(JBTable table) {
        assertFalse(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
        assertEquals(new Dimension(0, 0), table.getIntercellSpacing());
    }

    public void testVerifySignatureFeedbackTracksInputs() {
        Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        JwtPanel panel = new JwtPanel(getProject());
        try {
            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            JPanel signatureResult = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.result", JPanel.class);
            JLabel signatureMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.message", JLabel.class);
            JLabel signatureIcon = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.icon", JLabel.class);
            JPanel feedbackStack = findComponentNamed(panel.getRootPanel(), "jwt.feedback.stack", JPanel.class);
            JPanel expirationWarning = findComponentNamed(panel.getRootPanel(),
                    "jwt.expiration.warning", JPanel.class);
            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            JLabel missingInputMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.message", JLabel.class);
            JLabel missingInputIcon = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.icon", JLabel.class);
            assertNotNull(verifySignature);
            assertNotNull(signatureResult);
            assertNotNull(signatureMessage);
            assertNotNull(signatureIcon);
            assertNotNull(feedbackStack);
            assertNotNull(expirationWarning);
            assertNotNull(missingInputHint);
            assertNotNull(missingInputMessage);
            assertNotNull(missingInputIcon);
            assertSame(feedbackStack, expirationWarning.getParent());
            assertSame(feedbackStack, signatureResult.getParent());
            assertSame(feedbackStack, missingInputHint.getParent());
            assertEquals(0, feedbackStack.getComponentZOrder(expirationWarning));
            assertEquals(1, feedbackStack.getComponentZOrder(signatureResult));
            assertEquals(2, feedbackStack.getComponentZOrder(missingInputHint));
            assertEquals(expirationWarning.getBackground(), missingInputHint.getBackground());
            assertEquals(expirationWarning.getInsets(), missingInputHint.getInsets());
            assertEquals(AllIcons.General.BalloonWarning, missingInputIcon.getIcon());
            assertSame(missingInputHint, missingInputMessage.getParent());
            assertEquals(expirationWarning.getComponent(1).getForeground(), missingInputMessage.getForeground());
            assertFalse(verifySignature.isEnabled());
            assertFalse(signatureResult.isVisible());
            assertFalse(missingInputHint.isVisible());
            assertEquals("Paste a JWT to verify the signature", verifySignature.getToolTipText());
            assertEquals(verifySignature.getToolTipText(),
                    verifySignature.getAccessibleContext().getAccessibleDescription());

            panel.setJwt(new Jwt(signed.toString()));
            assertFalse(verifySignature.isEnabled());
            assertTrue(missingInputHint.isVisible());
            assertEquals("Enter a shared secret in the HS256 tab", missingInputMessage.getText());
            assertEquals(missingInputMessage.getText(), missingInputMessage.getToolTipText());
            assertEquals("Enter the shared secret to verify the signature", verifySignature.getToolTipText());
            assertEquals(verifySignature.getToolTipText(),
                    verifySignature.getAccessibleContext().getAccessibleDescription());
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            for (int tabIndex = 0; tabIndex < tabs.getTabCount(); tabIndex++) {
                tabs.setSelectedIndex(tabIndex);
                assertTrue("Missing-input guidance should remain visible on every tab", missingInputHint.isVisible());
            }

            HS256Panel secret = (HS256Panel) panel.getSecretPanel();
            secret.setSecret("wrong secret");
            assertTrue(verifySignature.isEnabled());
            assertFalse(missingInputHint.isVisible());
            verifySignature.doClick();
            assertTrue(signatureResult.isVisible());
            assertTrue(signatureMessage.getText().startsWith("Signature invalid:"));
            assertEquals(signatureMessage.getText(), signatureMessage.getToolTipText());
            assertSame(AllIcons.General.BalloonWarning, signatureIcon.getIcon());
            assertTrue(signatureResult.isOpaque());

            secret.setSecret("secret");
            assertFalse(signatureResult.isVisible());
            assertEquals("", signatureMessage.getText());
            verifySignature.doClick();
            assertTrue(signatureResult.isVisible());
            assertEquals("Signature verified", signatureMessage.getText());
            assertSame(AllIcons.General.InspectionsOK, signatureIcon.getIcon());
            assertFalse(signatureResult.isOpaque());

            panel.setSignatureContext(new HS256SignatureContext("updated secret"));
            assertFalse(signatureResult.isVisible());
            panel.setSignatureContext(new HS256SignatureContext("secret"));
            verifySignature.doClick();
            assertTrue(signatureResult.isVisible());

            panel.setJwt(new Jwt(signed.toString()));
            assertFalse(signatureResult.isVisible());
            secret.setSecret("another secret");
            assertFalse(signatureResult.isVisible());
            secret.setSecret("secret");
            verifySignature.doClick();
            assertTrue(signatureResult.isVisible());
            panel.reset();
            assertFalse(signatureResult.isVisible());
            assertFalse(missingInputHint.isVisible());
            assertEquals("Signature", tabs.getTitleAt(2));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testWarningFeedbackMatchesClaimBannerAndTracksThemeColors() {
        Color initialWarningBackground = new Color(0xFFE0A0);
        Color updatedWarningBackground = new Color(0xC08040);
        Color initialForeground = new Color(0x202020);
        Color updatedForeground = new Color(0xE0E0E0);
        Object previousWarningBackground = UIManager.put("Banner.warningBackground", initialWarningBackground);
        Object previousForeground = UIManager.put("Banner.foreground", initialForeground);
        JwtPanel panel = new JwtPanel(getProject());
        ClaimDialog dialog = new ClaimDialog(new StringClaim("subject", "alice"), ClaimDialog.Mode.VIEW,
                "Read-only: Enter a shared secret in the HS256 tab to edit claims.");
        try {
            JPanel expirationWarning = findComponentNamed(panel.getRootPanel(),
                    "jwt.expiration.warning", JPanel.class);
            JPanel signatureResult = findComponentNamed(panel.getRootPanel(), "jwt.signature.result", JPanel.class);
            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            JLabel expirationMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.expiration.message", JLabel.class);
            JLabel signatureMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.message", JLabel.class);
            JLabel missingInputMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.message", JLabel.class);
            EditorNotificationPanel claimBanner = findComponentNamed((Container) dialog.createCenterPanel(),
                    "jwt.claim.readOnly.banner", EditorNotificationPanel.class);
            JLabel claimBannerMessage = findLabelWithText((Container) claimBanner,
                    "Read-only: Enter a shared secret in the HS256 tab to edit claims.");

            assertNotNull(expirationWarning);
            assertNotNull(signatureResult);
            assertNotNull(missingInputHint);
            assertNotNull(expirationMessage);
            assertNotNull(signatureMessage);
            assertNotNull(missingInputMessage);
            assertNotNull(claimBanner);
            assertNotNull(claimBannerMessage);
            assertEquals(initialForeground.getRGB(), claimBannerMessage.getForeground().getRGB());
            assertEquals(initialForeground.getRGB(), expirationMessage.getForeground().getRGB());
            assertEquals(initialForeground.getRGB(), signatureMessage.getForeground().getRGB());
            assertEquals(initialForeground.getRGB(), missingInputMessage.getForeground().getRGB());
            assertWarningColorsMatchBanner(claimBanner, claimBannerMessage, expirationWarning, expirationMessage);
            assertWarningColorsMatchBanner(claimBanner, claimBannerMessage, signatureResult, signatureMessage);
            assertWarningColorsMatchBanner(claimBanner, claimBannerMessage, missingInputHint, missingInputMessage);

            UIManager.put("Banner.warningBackground", updatedWarningBackground);
            UIManager.put("Banner.foreground", updatedForeground);
            assertEquals(updatedWarningBackground.getRGB(), claimBanner.getBackground().getRGB());
            assertEquals(updatedWarningBackground.getRGB(), expirationWarning.getBackground().getRGB());
            assertEquals(updatedWarningBackground.getRGB(), signatureResult.getBackground().getRGB());
            assertEquals(updatedWarningBackground.getRGB(), missingInputHint.getBackground().getRGB());
            assertEquals(updatedForeground.getRGB(), claimBannerMessage.getForeground().getRGB());
            assertEquals(updatedForeground.getRGB(), expirationMessage.getForeground().getRGB());
            assertEquals(updatedForeground.getRGB(), signatureMessage.getForeground().getRGB());
            assertEquals(updatedForeground.getRGB(), missingInputMessage.getForeground().getRGB());
            assertWarningColorsMatchBanner(claimBanner, claimBannerMessage, expirationWarning, expirationMessage);
            assertWarningColorsMatchBanner(claimBanner, claimBannerMessage, signatureResult, signatureMessage);
            assertWarningColorsMatchBanner(claimBanner, claimBannerMessage, missingInputHint, missingInputMessage);
        } finally {
            UIManager.put("Banner.warningBackground", previousWarningBackground);
            UIManager.put("Banner.foreground", previousForeground);
            Disposer.dispose(panel);
        }
    }

    private static void assertWarningColorsMatchBanner(EditorNotificationPanel banner, JLabel bannerMessage,
                                                       JPanel feedbackPanel, JLabel feedbackMessage) {
        assertEquals(banner.getBackground().getRGB(), feedbackPanel.getBackground().getRGB());
        assertEquals(bannerMessage.getForeground().getRGB(), feedbackMessage.getForeground().getRGB());
    }

    private static JLabel findLabelWithText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel nested = findLabelWithText(child, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    public void testSuccessfulSignatureFeedbackFollowsThemeForeground() {
        Color lightForeground = new Color(0x202020);
        Color darkForeground = new Color(0xE0E0E0);
        Object previousForeground = UIManager.put("Label.foreground", lightForeground);
        try {
            JwtPanel panel = new JwtPanel(getProject());
            Disposer.register(getTestRootDisposable(), panel);
            HS256SignatureContext context = new HS256SignatureContext("secret");
            Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256, context, false);
            panel.setJwt(new Jwt(signed.toString()));
            panel.setSignatureContext(context);

            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            JLabel signatureMessage = findComponentNamed(panel.getRootPanel(), "jwt.signature.message", JLabel.class);
            assertNotNull(verifySignature);
            assertNotNull(signatureMessage);

            UIManager.put("Label.foreground", darkForeground);
            verifySignature.doClick();
            assertEquals("Signature verified", signatureMessage.getText());
            assertEquals(darkForeground.getRGB(), signatureMessage.getForeground().getRGB());

            UIManager.put("Label.foreground", lightForeground);
            assertEquals(lightForeground.getRGB(), signatureMessage.getForeground().getRGB());
        } finally {
            UIManager.put("Label.foreground", previousForeground);
        }
    }

    public void testUnsupportedAlgorithmShowsDisabledReason() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt("eyJhbGciOiJ1bmtub3duIn0.eyJzdWIiOiJhbGljZSJ9.c2ln"));
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertEquals("Signature", tabs.getTitleAt(2));

            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            assertNotNull(verifySignature);
            assertFalse(verifySignature.isEnabled());
            assertEquals("Unsupported algorithm unknown to verify the signature", verifySignature.getToolTipText());
            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            assertNotNull(missingInputHint);
            assertFalse(missingInputHint.isVisible());
            assertEquals(verifySignature.getToolTipText(),
                    verifySignature.getAccessibleContext().getAccessibleDescription());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testRsaVerificationShowsPublicKeyRequirement() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.c2ln"));

            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            assertNotNull(verifySignature);
            assertFalse(verifySignature.isEnabled());
            assertEquals("Enter the RSA public key to verify the signature", verifySignature.getToolTipText());
            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            JLabel missingInputMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.message", JLabel.class);
            assertNotNull(missingInputHint);
            assertNotNull(missingInputMessage);
            assertTrue(missingInputHint.isVisible());
            assertEquals("Enter the RSA public key in the RS256 tab", missingInputMessage.getText());
            assertEquals(verifySignature.getToolTipText(),
                    verifySignature.getAccessibleContext().getAccessibleDescription());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testHs384MissingSecretHintClearsForInputAndUnsupportedToken() {
        Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS384,
                new HS384SignatureContext("secret"), false);
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt(signed.toString()));

            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            JLabel missingInputMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.message", JLabel.class);
            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            assertNotNull(missingInputHint);
            assertNotNull(missingInputMessage);
            assertNotNull(verifySignature);
            assertTrue(missingInputHint.isVisible());
            assertEquals("Enter a shared secret in the HS384 tab", missingInputMessage.getText());
            assertFalse(verifySignature.isEnabled());

            ((HS384Panel) panel.getSecretPanel()).setSecret("secret");
            assertFalse(missingInputHint.isVisible());
            assertTrue(verifySignature.isEnabled());

            panel.setJwt(new Jwt("eyJhbGciOiJ1bmtub3duIn0.eyJzdWIiOiJhbGljZSJ9.c2ln"));
            assertFalse(missingInputHint.isVisible());
            assertFalse(verifySignature.isEnabled());

            panel.clearParsedJwtKeepingSigningInput();
            assertFalse(missingInputHint.isVisible());
            panel.reset();
            assertFalse(missingInputHint.isVisible());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testMalformedRsaKeyShowsSignatureWarning() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.c2ln"));
            RS256Panel rsaPanel = (RS256Panel) panel.getSecretPanel();
            ((EditorTextField) rsaPanel.getPublicKeyInput()).setText("malformed public key");

            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            JPanel signatureResult = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.result", JPanel.class);
            JLabel signatureMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.message", JLabel.class);
            JLabel signatureIcon = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.icon", JLabel.class);
            assertNotNull(verifySignature);
            assertNotNull(signatureResult);
            assertNotNull(signatureMessage);
            assertTrue(verifySignature.isEnabled());
            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            assertNotNull(missingInputHint);
            assertFalse(missingInputHint.isVisible());

            verifySignature.doClick();

            assertTrue(signatureResult.isVisible());
            assertTrue(signatureMessage.getText().startsWith("Signature verification failed:"));
            assertSame(AllIcons.General.BalloonWarning, signatureIcon.getIcon());
            assertTrue(signatureResult.isOpaque());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testExpiredTokenAndSignatureFeedbackAreIndependent() {
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
            JPanel feedbackStack = findComponentNamed(panel.getRootPanel(), "jwt.feedback.stack", JPanel.class);
            JPanel signatureResult = findComponentNamed(panel.getRootPanel(), "jwt.signature.result", JPanel.class);
            JPanel missingInputHint = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.hint", JPanel.class);
            JLabel missingInputMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.verification.input.message", JLabel.class);
            assertTrue(feedbackStack.isVisible());
            assertFalse(signatureResult.isVisible());
            assertNotNull(missingInputHint);
            assertNotNull(missingInputMessage);
            assertTrue(missingInputHint.isVisible());
            assertEquals("Enter a shared secret in the HS256 tab", missingInputMessage.getText());
            assertSame(feedbackStack, warning.getParent());
            assertSame(feedbackStack, signatureResult.getParent());
            assertSame(feedbackStack, missingInputHint.getParent());
            Container parsedContent = feedbackStack.getParent();
            assertTrue(parsedContent.getLayout() instanceof BorderLayout);
            assertSame(feedbackStack,
                    ((BorderLayout) parsedContent.getLayout()).getLayoutComponent(BorderLayout.NORTH));
            assertEquals(0, feedbackStack.getComponentZOrder(warning));
            assertEquals(1, feedbackStack.getComponentZOrder(signatureResult));
            assertEquals(2, feedbackStack.getComponentZOrder(missingInputHint));
            JLabel message = findComponentNamed(warning, "jwt.expiration.message", JLabel.class);
            assertNotNull(message);
            assertEquals("Token expired at " + expiration, message.getText());

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            tabs.setSelectedIndex(1);
            assertTrue(warning.isVisible());
            assertTrue(missingInputHint.isVisible());

            HS256Panel secretPanel = (HS256Panel) panel.getSecretPanel();
            secretPanel.setSecret("wrong secret");
            assertFalse(missingInputHint.isVisible());
            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            JLabel signatureMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.message", JLabel.class);
            JLabel signatureIcon = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.icon", JLabel.class);
            assertTrue(verifySignature.isEnabled());
            verifySignature.doClick();
            assertTrue(signatureResult.isVisible());
            assertTrue(signatureMessage.getText().startsWith("Signature invalid:"));
            assertSame(AllIcons.General.BalloonWarning, signatureIcon.getIcon());
            assertTrue(warning.isVisible());

            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            int expirationRow = findRow(claims, "exp");
            assertTrue(expirationRow >= 0);
            assertTrue(hasErrorIcon(claims, expirationRow, 1));

            assertTrue(warning.isVisible());
            assertTrue(signatureResult.isVisible());
            secretPanel.setSecret("secret");
            assertFalse(missingInputHint.isVisible());
            assertFalse(signatureResult.isVisible());
            assertTrue(warning.isVisible());
            verifySignature.doClick();
            assertTrue(warning.isVisible());
            assertTrue(signatureResult.isVisible());
            assertEquals("Signature verified", signatureMessage.getText());
            assertSame(AllIcons.General.InspectionsOK, signatureIcon.getIcon());
            assertFalse(signatureResult.isOpaque());
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

    public void testVerifySignatureDoesNotRefreshTimeClaimsBeforeScheduledCheck() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Instant expiration = now.plusSeconds(2);
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithExpiration(NumericDate.fromInstant(expiration)).toString()));
            panel.setSignatureContext(new HS256SignatureContext("secret"));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertNotNull(warning);
            assertFalse(warning.isVisible());

            clock.setInstant(expiration);
            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            JPanel signatureResult = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.result", JPanel.class);
            JLabel signatureMessage = findComponentNamed(panel.getRootPanel(),
                    "jwt.signature.message", JLabel.class);
            verifySignature.doClick();

            assertFalse(warning.isVisible());
            assertTrue(signatureResult.isVisible());
            assertEquals("Signature verified", signatureMessage.getText());
            scheduler.runScheduledTask();
            assertTrue(warning.isVisible());
            assertTrue(signatureResult.isVisible());
            assertEquals("Signature verified", signatureMessage.getText());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testVisibleExpirationWarningFollowsTheTimestampFormat() {
        TimeZone originalZone = TimeZone.getDefault();
        Locale originalLocale = Locale.getDefault(Locale.Category.FORMAT);
        Configuration.TimestampFormat originalFormat = Configuration.INSTANCE.getTimestampFormat();
        Instant expiration = Instant.parse("2025-07-01T00:00:00.125Z");
        BigDecimal expirationSeconds = NumericDate.fromInstant(expiration);
        MutableClock clock = new MutableClock(expiration);
        JwtPanel panel = new JwtPanel(getProject(), clock, new ManualExpirationScheduler());
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH);
            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO);
            panel.setJwt(new Jwt(createJwtWithExpiration(expirationSeconds).toString()));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            JLabel message = findComponentNamed(warning, "jwt.expiration.message", JLabel.class);
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            int expirationRow = findRow(claims, "exp");
            claims.setRowSelectionInterval(expirationRow, expirationRow);
            assertEquals("Token expired at " + expiration, message.getText());

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RELATIVE);
            assertEquals("Token expired just now", message.getText());

            clock.setInstant(expiration.plusSeconds(2 * 3600));
            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RAW);
            assertEquals("Token expired at 1751328000.125", message.getText());

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO_LOCAL);
            assertEquals("Token expired at 2025-06-30T20:00:00.125-04:00", message.getText());
            assertEquals("2025-06-30T20:00:00.125-04:00", claims.getValueAt(expirationRow, 1));

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.MEDIUM);
            String medium = TimestampFormatter.format(expirationSeconds, Configuration.TimestampFormat.MEDIUM,
                    clock.instant());
            assertEquals("Token expired at " + medium, message.getText());
            assertEquals(medium, claims.getValueAt(expirationRow, 1));
            assertEquals(message.getText(), panel.getClaimErrorAtViewCell(expirationRow, 1).getMessage());
            assertEquals(expirationRow, claims.getSelectedRow());

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO);
            assertEquals("Token expired at " + expiration, message.getText());
            assertEquals(expiration.toString(), claims.getValueAt(expirationRow, 1));
            assertEquals(message.getText(), panel.getClaimErrorAtViewCell(expirationRow, 1).getMessage());
            assertEquals(expirationRow, claims.getSelectedRow());

            Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RELATIVE);
            assertEquals("Token expired 2 hours ago", message.getText());
            assertEquals(message.getText(), panel.getClaimErrorAtViewCell(expirationRow, 1).getMessage());
            assertTrue(warning.isVisible());
            assertEquals(expirationRow, claims.getSelectedRow());
        } finally {
            Disposer.dispose(panel);
            Configuration.INSTANCE.setTimestampFormat(originalFormat);
            Locale.setDefault(Locale.Category.FORMAT, originalLocale);
            TimeZone.setDefault(originalZone);
        }
    }

    public void testExpirationValidationAndWarningUseOneClockSnapshot() {
        Instant expiration = Instant.parse("2025-01-01T00:00:02Z");
        AdvancingClock clock = new AdvancingClock(expiration.minusNanos(1));
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithExpiration(NumericDate.fromInstant(expiration)).toString()));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertNotNull(warning);
            assertFalse(warning.isVisible());
            assertEquals(1, clock.readCount);
            assertEquals(1, scheduler.delayMillis);

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            int expirationRow = findRow(claims, "exp");
            assertFalse(hasErrorIcon(claims, expirationRow, 1));

            scheduler.runScheduledTask();

            assertEquals(2, clock.readCount);
            assertTrue(warning.isVisible());
            assertTrue(hasErrorIcon(claims, expirationRow, 1));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testFarFutureExpirationUsesCappedRefreshAndNumericFallback() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            BigDecimal expiration = new BigDecimal("1E+2147483647");
            panel.setJwt(new Jwt(createJwtWithExpiration(expiration).toString()));

            assertEquals(Integer.MAX_VALUE, scheduler.delayMillis);
            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertNotNull(warning);
            assertFalse(warning.isVisible());

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertEquals(expiration.toString(), claims.getValueAt(findRow(claims, "exp"), 1));
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testNotBeforeRefreshesWithoutExpiration() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Instant notBefore = now.plusSeconds(2);
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithTimeClaims(null, NumericDate.fromInstant(notBefore), true).toString()));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            int notBeforeRow = findRow(claims, "nbf");
            assertNotNull(warning);
            assertFalse(warning.isVisible());
            assertTrue(hasErrorIcon(claims, notBeforeRow, 1));
            assertEquals(2_000, scheduler.delayMillis);

            clock.setInstant(notBefore);
            scheduler.runScheduledTask();

            assertFalse(warning.isVisible());
            assertFalse(hasErrorIcon(claims, notBeforeRow, 1));
            assertNull(scheduler.scheduledTask);
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testBothFutureClaimsScheduleTheEarliestDeadlineRegardlessOfClaimOrder() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithTimeClaims(
                    NumericDate.fromInstant(now.plusSeconds(5)), NumericDate.fromInstant(now.plusSeconds(2)), true)
                    .toString()));
            assertEquals(2_000, scheduler.delayMillis);

            panel.setJwt(new Jwt(createJwtWithTimeClaims(
                    NumericDate.fromInstant(now.plusSeconds(2)), NumericDate.fromInstant(now.plusSeconds(5)), false)
                    .toString()));
            assertEquals(2_000, scheduler.delayMillis);
            assertEquals(2, scheduler.scheduleCount);
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testEqualExpirationAndNotBeforeDeadlinesUseOneRefresh() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Instant deadline = now.plusSeconds(3);
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            BigDecimal timestamp = NumericDate.fromInstant(deadline);
            panel.setJwt(new Jwt(createJwtWithTimeClaims(timestamp, timestamp, true).toString()));
            assertEquals(3_000, scheduler.delayMillis);
            assertEquals(1, scheduler.scheduleCount);

            clock.setInstant(deadline);
            scheduler.runScheduledTask();

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertTrue(hasErrorIcon(claims, findRow(claims, "exp"), 1));
            assertFalse(hasErrorIcon(claims, findRow(claims, "nbf"), 1));
            assertEquals(1, scheduler.scheduleCount);
            assertNull(scheduler.scheduledTask);
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testMalformedExpirationDoesNotPreventNotBeforeRefresh() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Instant notBefore = now.plusSeconds(2);
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        Jwt malformedExpiration = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        malformedExpiration.setPayloadClaim(new StringClaim("exp", "not a number"));
        malformedExpiration.setPayloadClaim(DateClaim.fromNumericDate("nbf", NumericDate.fromInstant(notBefore)));
        malformedExpiration.rebuild();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(malformedExpiration.toString()));
            assertEquals(2_000, scheduler.delayMillis);

            clock.setInstant(notBefore);
            scheduler.runScheduledTask();

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertTrue(hasErrorIcon(claims, findRow(claims, "exp"), 1));
            assertFalse(hasErrorIcon(claims, findRow(claims, "nbf"), 1));
            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertFalse(warning.isVisible());
            assertNull(scheduler.scheduledTask);
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testExpiredExpirationDoesNotPreventNotBeforeRefresh() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Instant expiration = now.minusSeconds(1);
        Instant notBefore = now.plusSeconds(2);
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithTimeClaims(
                    NumericDate.fromInstant(expiration), NumericDate.fromInstant(notBefore), true).toString()));

            JPanel warning = findComponentNamed(panel.getRootPanel(), "jwt.expiration.warning", JPanel.class);
            assertTrue(warning.isVisible());
            assertEquals(2_000, scheduler.delayMillis);

            clock.setInstant(notBefore);
            scheduler.runScheduledTask();

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            assertTrue(warning.isVisible());
            assertTrue(hasErrorIcon(claims, findRow(claims, "exp"), 1));
            assertFalse(hasErrorIcon(claims, findRow(claims, "nbf"), 1));
            assertNull(scheduler.scheduledTask);
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testClaimRefreshIsCancelledOnReplacementAndClearing() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), clock, scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithTimeClaims(null,
                    NumericDate.fromInstant(now.plusSeconds(10)), true).toString()));
            Runnable staleTask = scheduler.scheduledTask;
            assertNotNull(staleTask);

            panel.setJwt(new Jwt(createJwtWithTimeClaims(null,
                    NumericDate.fromInstant(now.plusSeconds(20)), true).toString()));
            assertNotNull(scheduler.scheduledTask);
            assertNotSame(staleTask, scheduler.scheduledTask);
            assertEquals(20_000, scheduler.delayMillis);

            panel.clearParsedJwtKeepingSigningInput();
            assertNull(scheduler.scheduledTask);
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testDisposingPanelCancelsClaimRefresh() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        ManualExpirationScheduler scheduler = new ManualExpirationScheduler();
        JwtPanel panel = new JwtPanel(getProject(), new MutableClock(now), scheduler);
        try {
            panel.setJwt(new Jwt(createJwtWithTimeClaims(null,
                    NumericDate.fromInstant(now.plusSeconds(10)), true).toString()));
            assertNotNull(scheduler.scheduledTask);
        } finally {
            Disposer.dispose(panel);
        }
        assertNull(scheduler.scheduledTask);
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
            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            assertTrue(verifySignature.isEnabled());

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
            assertFalse(verifySignature.isEnabled());

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
            JButton verifySignature = findComponentNamed(panel.getVerificationToolbarComponent(),
                    "jwt.verifySignature.button", JButton.class);
            assertTrue(verifySignature.isEnabled());
            verifySignature.doClick();
            assertFalse(hasErrorIcon(claims, issuedAtRow, 1));

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

    public void testKeyboardClaimErrorFollowsLeadRowAndValueColumn() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(new Jwt(createJwtWithIssuedAt("yes").toString()));

            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            JBTable claims = UIUtil.findComponentOfType((JComponent) tabs.getComponentAt(0), JBTable.class);
            int issuedAtRow = findRow(claims, "iat");
            int subjectRow = findRow(claims, "subject");
            int firstRow = Math.min(issuedAtRow, subjectRow);
            int lastRow = Math.max(issuedAtRow, subjectRow);
            JwtClaimsTableModel model = (JwtClaimsTableModel) claims.getModel();
            ClaimError firstError = new ClaimError(model.getClaimAt(claims.convertRowIndexToModel(firstRow)).getName(),
                    "First selected row error");
            ClaimError leadError = new ClaimError(model.getClaimAt(claims.convertRowIndexToModel(lastRow)).getName(),
                    "Lead row error");
            model.setClaimErrors(java.util.List.of(firstError, leadError));

            int valueColumn = claims.convertColumnIndexToView(1);
            claims.moveColumn(valueColumn, 0);
            claims.setRowSelectionInterval(firstRow, lastRow);
            assertEquals(firstRow, claims.getSelectedRow());
            assertEquals(lastRow, claims.getSelectionModel().getLeadSelectionIndex());
            assertSame(leadError, panel.getClaimErrorAtViewCell(lastRow, claims.convertColumnIndexToView(1)));
            assertSame(leadError, panel.getClaimErrorAtFocusedRow());

            claims.setRowSelectionInterval(lastRow, firstRow);
            assertEquals(firstRow, claims.getSelectedRow());
            assertEquals(firstRow, claims.getSelectionModel().getLeadSelectionIndex());
            assertSame(firstError, panel.getClaimErrorAtFocusedRow());

            model.setClaimErrors(java.util.List.of(firstError));
            claims.setRowSelectionInterval(firstRow, lastRow);
            assertNull(panel.getClaimErrorAtFocusedRow());

            claims.clearSelection();
            assertNull(panel.getClaimErrorAtFocusedRow());
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

    private static Jwt createJwtWithTimeClaims(BigDecimal expiration, BigDecimal notBefore,
                                               boolean expirationFirst) {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("secret"), false);
        if (expirationFirst) {
            if (expiration != null) {
                jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", expiration));
            }
            if (notBefore != null) {
                jwt.setPayloadClaim(DateClaim.fromNumericDate("nbf", notBefore));
            }
        } else {
            if (notBefore != null) {
                jwt.setPayloadClaim(DateClaim.fromNumericDate("nbf", notBefore));
            }
            if (expiration != null) {
                jwt.setPayloadClaim(DateClaim.fromNumericDate("exp", expiration));
            }
        }
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

    private static final class AdvancingClock extends Clock {
        private Instant instant;
        private int readCount;

        private AdvancingClock(Instant instant) {
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
            Instant result = this.instant;
            this.instant = this.instant.plusNanos(1);
            this.readCount++;
            return result;
        }
    }

    private static final class ManualExpirationScheduler implements JwtPanel.ClaimRefreshScheduler {
        private long delayMillis;
        private int scheduleCount;
        private Runnable scheduledTask;

        @Override
        public void schedule(long delayMillis, Runnable task) {
            this.delayMillis = delayMillis;
            this.scheduleCount++;
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
