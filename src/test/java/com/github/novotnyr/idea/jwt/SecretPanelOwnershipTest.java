package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.hs384.HS384Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.github.novotnyr.idea.jwt.ui.preferences.PluginPreferences;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.UIUtil;
import org.jspecify.annotations.NonNull;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SecretPanelOwnershipTest extends LightPlatformTestCase {
    public void testUnrecognizedPanelKeepsAttachedBalloonTargetAcrossTokenSwitches() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            UnrecognizedSecretPanel unrecognized = (UnrecognizedSecretPanel) panel.getSecretPanel();
            JComponent root = unrecognized.getRoot();
            Container parent = root.getParent();
            assertNotNull(parent);
            assertSame(root, unrecognized.getBaloonableComponent());

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            assertNull(root.getParent());

            panel.setJwt(Jwt.EMPTY);
            assertSame(unrecognized, panel.getSecretPanel());
            assertSame(root, unrecognized.getRoot());
            assertSame(root, unrecognized.getBaloonableComponent());
            assertSame(parent, root.getParent());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testTwoToolWindowsKeepIndependentPanelsAndSecrets() {
        JwtPanel first = new JwtPanel(getProject());
        JwtPanel second = new JwtPanel(getProject());
        try {
            Jwt hs256Token = tokenWithIat(false);
            first.setJwt(hs256Token);
            second.setJwt(hs256Token);

            HS256Panel firstSecret = (HS256Panel) first.getSecretPanel();
            HS256Panel secondSecret = (HS256Panel) second.getSecretPanel();
            assertNotSame(firstSecret, secondSecret);
            assertNotSame(firstSecret.getRoot(), secondSecret.getRoot());
            assertNotSame(firstSecret.getRoot().getParent(), secondSecret.getRoot().getParent());

            firstSecret.setSecret("first window");
            secondSecret.setSecret("second window");
            assertEquals("first window", ((HS256SignatureContext) firstSecret.getSignatureContext()).secret());
            assertEquals("second window", ((HS256SignatureContext) secondSecret.getSignatureContext()).secret());

            first.setJwt(tokenWithIat(true));
            assertSame(firstSecret, first.getSecretPanel());
            assertEquals("first window", ((HS256SignatureContext) first.getSecretPanel().getSignatureContext()).secret());
            assertEquals("second window", ((HS256SignatureContext) second.getSecretPanel().getSignatureContext()).secret());
        } finally {
            Disposer.dispose(first);
            Disposer.dispose(second);
        }
    }

    public void testAlgorithmSwitchKeepsEachValueAndResetClearsAll() {
        JwtPanel panel = new JwtPanel(getProject());
        try {
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            HS256Panel hs256 = (HS256Panel) panel.getSecretPanel();
            hs256.setSecret("hs256 value");

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS384));
            HS384Panel hs384 = (HS384Panel) panel.getSecretPanel();
            hs384.setSecret("hs384 value");

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel rs256 = (RS256Panel) panel.getSecretPanel();

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            assertSame(hs256, panel.getSecretPanel());
            assertEquals("hs256 value", ((HS256SignatureContext) hs256.getSignatureContext()).secret());
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS384));
            assertSame(hs384, panel.getSecretPanel());

            panel.reset();
            assertTrue(Disposer.isDisposed(hs256));
            assertTrue(Disposer.isDisposed(hs384));
            assertTrue(Disposer.isDisposed(rs256));

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            assertNotSame(hs256, panel.getSecretPanel());
            assertEquals("", ((HS256SignatureContext) panel.getSecretPanel().getSignatureContext()).secret());
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            assertNotSame(rs256, panel.getSecretPanel());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testTabSwitchesKeepCachedEditorsInTheKeySecretTab() {
        ManualExecutor executor = new ManualExecutor();
        JwtPanel panel = new JwtPanel(getProject(), executor);
        panel.getRootPanel().addNotify();
        try {
            JTabbedPane tabs = UIUtil.findComponentOfType(panel.getRootPanel(), JTabbedPane.class);
            assertNotNull(tabs);
            assertEquals(3, tabs.getTabCount());
            assertEquals("Signature", tabs.getTitleAt(2));

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            assertEquals("HS256", tabs.getTitleAt(2));
            HS256Panel hs256 = (HS256Panel) panel.getSecretPanel();
            assertTrue(SwingUtilities.isDescendingFrom(hs256.getRoot(), tabs.getComponentAt(2)));
            assertFalse(hs256.getRoot().isShowing());
            tabs.setSelectedIndex(2);
            assertTrue(hs256.getRoot().isShowing());
            hs256.setSecret("cached draft");
            tabs.setSelectedIndex(0);
            assertFalse(hs256.getRoot().isShowing());
            tabs.setSelectedIndex(2);
            assertSame(hs256, panel.getSecretPanel());
            assertEquals("cached draft", ((HS256SignatureContext) hs256.getSignatureContext()).secret());

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS384));
            assertEquals("HS384", tabs.getTitleAt(2));
            HS384Panel hs384 = (HS384Panel) panel.getSecretPanel();
            assertFalse(SwingUtilities.isDescendingFrom(hs256.getRoot(), tabs.getComponentAt(2)));
            assertTrue(SwingUtilities.isDescendingFrom(hs384.getRoot(), tabs.getComponentAt(2)));
            tabs.setSelectedIndex(2);
            assertTrue(hs384.getRoot().isShowing());

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            assertEquals("RS256", tabs.getTitleAt(2));
            RS256Panel rs256 = (RS256Panel) panel.getSecretPanel();
            List<EditorTextField> rsaFields = findComponents(rs256.getRoot(), EditorTextField.class);
            rsaFields.get(0).setText("public key draft");
            rsaFields.get(1).setText("private key draft");
            assertEquals(0, tabs.getSelectedIndex());
            tabs.setSelectedIndex(2);
            assertTrue(SwingUtilities.isDescendingFrom(rs256.getRoot(), tabs.getComponentAt(2)));
            assertTrue(rs256.getRoot().isShowing());

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            assertEquals(0, tabs.getSelectedIndex());
            assertEquals("HS256", tabs.getTitleAt(2));
            assertSame(hs256, panel.getSecretPanel());
            assertTrue(SwingUtilities.isDescendingFrom(hs256.getRoot(), tabs.getComponentAt(2)));
            assertEquals("cached draft", ((HS256SignatureContext) hs256.getSignatureContext()).secret());
            assertFalse(SwingUtilities.isDescendingFrom(rs256.getRoot(), tabs.getComponentAt(2)));

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            assertEquals(0, tabs.getSelectedIndex());
            assertEquals("RS256", tabs.getTitleAt(2));
            tabs.setSelectedIndex(2);
            assertSame(rs256, panel.getSecretPanel());
            assertTrue(SwingUtilities.isDescendingFrom(rs256.getRoot(), tabs.getComponentAt(2)));
            assertEquals("public key draft", rsaFields.get(0).getText());
            assertEquals("private key draft", rsaFields.get(1).getText());

            panel.setJwt(new Jwt("eyJhbGciOiJ1bmtub3duIn0.eyJzdWIiOiJhbGljZSJ9.c2ln"));
            assertEquals("Signature", tabs.getTitleAt(2));
        } finally {
            panel.getRootPanel().removeNotify();
            Disposer.dispose(panel);
        }
    }

    public void testListenerReplacementAndRemovalAreIdempotent() {
        HS256Panel panel = new HS256Panel();
        try {
            AtomicInteger notifications = new AtomicInteger();
            panel.setSignatureContextChangedListener(notifications::incrementAndGet);
            panel.setSignatureContextChangedListener(notifications::incrementAndGet);
            findComponents(panel.getRoot(), JTextField.class).getFirst().setText("one edit");
            assertEquals(1, notifications.get());

            panel.removeSignatureContextChangedListener();
            panel.removeSignatureContextChangedListener();
            panel.setSecret("another edit");
            assertEquals(1, notifications.get());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testRsaListenerReplacementAndRemovalAreIdempotent() throws Exception {
        RS256Panel panel = new RS256Panel(getProject(), SignatureContext.EMPTY);
        try {
            AtomicInteger notifications = new AtomicInteger();
            panel.setSignatureContextChangedListener(notifications::incrementAndGet);
            panel.setSignatureContextChangedListener(notifications::incrementAndGet);

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            String publicKey = RsaUtils.toString((RSAPublicKey) generator.generateKeyPair().getPublic());
            EditorTextField publicField = findComponents(panel.getRoot(), EditorTextField.class).getFirst();
            publicField.setText(publicKey);
            assertEquals(1, notifications.get());

            panel.removeSignatureContextChangedListener();
            panel.removeSignatureContextChangedListener();
            publicField.setText("unfinished edit");
            assertEquals(1, notifications.get());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testLateDefaultKeysDoNotOverwriteEditsOrReset() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        var pair = generator.generateKeyPair();
        String publicKey = RsaUtils.toString((RSAPublicKey) pair.getPublic());
        String privateKey = RsaUtils.toString((RSAPrivateKey) pair.getPrivate());
        Path publicFile = Files.createTempFile("jwt-public-", ".pem");
        Path privateFile = Files.createTempFile("jwt-private-", ".pem");
        Files.writeString(publicFile, publicKey);
        Files.writeString(privateFile, privateKey);

        PluginPreferences preferences = PluginPreferences.getInstance(getProject());
        String previousPublic = preferences.getRs256PublicKeyFile();
        String previousPrivate = preferences.getRs256PrivateKeyFile();
        preferences.setRs256PublicKeyFile(publicFile.toString());
        preferences.setRs256PrivateKeyFile(privateFile.toString());
        ManualExecutor executor = new ManualExecutor();
        JwtPanel panel = new JwtPanel(getProject(), executor);
        try {
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel first = (RS256Panel) panel.getSecretPanel();
            List<EditorTextField> firstFields = findComponents(first.getRoot(), EditorTextField.class);
            firstFields.getFirst().setText("manual public key draft");
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertEquals("manual public key draft", firstFields.get(0).getText());
            assertEquals("", firstFields.get(1).getText());

            panel.reset();
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel second = (RS256Panel) panel.getSecretPanel();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            List<EditorTextField> secondFields = findComponents(second.getRoot(), EditorTextField.class);
            assertFalse(secondFields.get(0).getText().isEmpty());
            assertFalse(secondFields.get(1).getText().isEmpty());

            panel.reset();
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel third = (RS256Panel) panel.getSecretPanel();
            panel.reset();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertTrue(Disposer.isDisposed(third));
            assertNotSame(third, panel.getSecretPanel());
        } finally {
            Disposer.dispose(panel);
            preferences.setRs256PublicKeyFile(previousPublic);
            preferences.setRs256PrivateKeyFile(previousPrivate);
            Files.deleteIfExists(publicFile);
            Files.deleteIfExists(privateFile);
        }
    }

    public void testSettingsChangeRefreshesOnlyUntouchedRsaDefaults() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        var firstPair = generator.generateKeyPair();
        var secondPair = generator.generateKeyPair();
        String firstPublic = RsaUtils.toString((RSAPublicKey) firstPair.getPublic());
        String firstPrivate = RsaUtils.toString((RSAPrivateKey) firstPair.getPrivate());
        String secondPublic = RsaUtils.toString((RSAPublicKey) secondPair.getPublic());
        String secondPrivate = RsaUtils.toString((RSAPrivateKey) secondPair.getPrivate());
        Path firstPublicFile = Files.createTempFile("jwt-first-public-", ".pem");
        Path firstPrivateFile = Files.createTempFile("jwt-first-private-", ".pem");
        Path secondPublicFile = Files.createTempFile("jwt-second-public-", ".pem");
        Path secondPrivateFile = Files.createTempFile("jwt-second-private-", ".pem");
        Files.writeString(firstPublicFile, firstPublic);
        Files.writeString(firstPrivateFile, firstPrivate);
        Files.writeString(secondPublicFile, secondPublic);
        Files.writeString(secondPrivateFile, secondPrivate);

        PluginPreferences preferences = PluginPreferences.getInstance(getProject());
        String previousPublic = preferences.getRs256PublicKeyFile();
        String previousPrivate = preferences.getRs256PrivateKeyFile();
        preferences.setRs256PublicKeyFile(firstPublicFile.toString());
        preferences.setRs256PrivateKeyFile(firstPrivateFile.toString());
        ManualExecutor executor = new ManualExecutor();
        JwtPanel panel = new JwtPanel(getProject(), executor);
        try {
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel rsa = (RS256Panel) panel.getSecretPanel();
            List<EditorTextField> fields = findComponents(rsa.getRoot(), EditorTextField.class);

            preferences.updateKeyFiles(secondPublicFile.toString(), secondPrivateFile.toString());
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertEquals("", fields.getFirst().getText());
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertEquals(secondPublic, fields.get(0).getText());
            assertEquals(secondPrivate, fields.get(1).getText());

            fields.get(0).setText("my public key draft");
            preferences.updateKeyFiles(firstPublicFile.toString(), firstPrivateFile.toString());
            assertEquals("my public key draft", fields.get(0).getText());
            assertEquals(secondPrivate, fields.get(1).getText());
        } finally {
            Disposer.dispose(panel);
            preferences.setRs256PublicKeyFile(previousPublic);
            preferences.setRs256PrivateKeyFile(previousPrivate);
            Files.deleteIfExists(firstPublicFile);
            Files.deleteIfExists(firstPrivateFile);
            Files.deleteIfExists(secondPublicFile);
            Files.deleteIfExists(secondPrivateFile);
        }
    }

    public void testRsaClaimMutationSignsWithThePanelKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        var pair = generator.generateKeyPair();
        RS256SignatureContext context = new RS256SignatureContext.Builder()
                .withPublicKey(RsaUtils.toString((RSAPublicKey) pair.getPublic()))
                .withPrivateKey(RsaUtils.toString((RSAPrivateKey) pair.getPrivate()))
                .build();
        Jwt original = JwtFactory.getInstance().newJwt(SignatureAlgorithm.RS256, context, false);

        JwtPanel panel = new JwtPanel(getProject(), new ManualExecutor());
        try {
            panel.setJwt(original);
            panel.setSignatureContext(context);
            AtomicReference<Jwt> updated = new AtomicReference<>();
            panel.addJwtListener(event -> updated.set((Jwt) event.getNewValue()));

            panel.updateClaim(new StringClaim("subject", "alice"), null);

            assertNotNull(updated.get());
            assertEquals("alice", JWT.decode(updated.get().toString()).getClaim("subject").asString());
            Algorithm.RSA256((RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate())
                    .verify(JWT.decode(updated.get().toString()));
        } finally {
            Disposer.dispose(panel);
        }
    }

    private static Jwt jwtForAlgorithm(String algorithm) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"alg\":\"" + algorithm + "\"}").getBytes(StandardCharsets.UTF_8));
        return new Jwt(header + ".e30.AQ");
    }

    private static Jwt tokenWithIat(boolean addIat) {
        Jwt signed = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("signing secret"), addIat);
        return new Jwt(signed.toString());
    }

    static <T extends Component> List<T> findComponents(Component root, Class<T> type) {
        List<T> components = new ArrayList<>();
        if (type.isInstance(root)) {
            components.add(type.cast(root));
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                components.addAll(findComponents(child, type));
            }
        }
        return components;
    }

    private static class ManualExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(@NonNull Runnable task) {
            this.tasks.add(task);
        }

        void runNext() {
            assertFalse(this.tasks.isEmpty());
            this.tasks.remove().run();
        }
    }
}
