package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.hs384.HS384Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.EditorTextField;

import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SecretPanelOwnershipTest extends LightPlatformTestCase {
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
            assertEquals("first window", ((HS256SignatureContext) firstSecret.getSignatureContext()).getSecret());
            assertEquals("second window", ((HS256SignatureContext) secondSecret.getSignatureContext()).getSecret());

            first.setJwt(tokenWithIat(true));
            assertSame(firstSecret, first.getSecretPanel());
            assertEquals("first window", ((HS256SignatureContext) first.getSecretPanel().getSignatureContext()).getSecret());
            assertEquals("second window", ((HS256SignatureContext) second.getSecretPanel().getSignatureContext()).getSecret());
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
            assertEquals("hs256 value", ((HS256SignatureContext) hs256.getSignatureContext()).getSecret());
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS384));
            assertSame(hs384, panel.getSecretPanel());

            panel.reset();
            assertTrue(Disposer.isDisposed(hs256));
            assertTrue(Disposer.isDisposed(hs384));
            assertTrue(Disposer.isDisposed(rs256));

            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            assertNotSame(hs256, panel.getSecretPanel());
            assertEquals("", ((HS256SignatureContext) panel.getSecretPanel().getSignatureContext()).getSecret());
            panel.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            assertNotSame(rs256, panel.getSecretPanel());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testListenerReplacementAndRemovalAreIdempotent() {
        HS256Panel panel = new HS256Panel();
        try {
            AtomicInteger notifications = new AtomicInteger();
            panel.setSignatureContextChangedListener(context -> notifications.incrementAndGet());
            panel.setSignatureContextChangedListener(context -> notifications.incrementAndGet());
            findComponents(panel.getRoot(), JTextField.class).get(0).setText("one edit");
            assertEquals(1, notifications.get());

            panel.removeSignatureContextChangedListener();
            panel.removeSignatureContextChangedListener();
            panel.setSecret("another edit");
            assertEquals(1, notifications.get());
        } finally {
            Disposer.dispose(panel);
        }
    }

    public void testRsaCopyRetainsUnfinishedRawTextWithoutParsing() {
        RS256Panel source = new RS256Panel(getProject(), SignatureContext.EMPTY);
        SecretPanel copy = null;
        try {
            List<EditorTextField> sourceFields = findComponents(source.getRoot(), EditorTextField.class);
            sourceFields.get(0).setText("unfinished public key");
            sourceFields.get(1).setText("unfinished private key");

            copy = source.copyFor(getProject());
            List<EditorTextField> copyFields = findComponents(copy.getRoot(), EditorTextField.class);
            assertNotSame(source.getRoot(), copy.getRoot());
            assertNotSame(sourceFields.get(0).getDocument(), copyFields.get(0).getDocument());
            assertEquals("unfinished public key", copyFields.get(0).getText());
            assertEquals("unfinished private key", copyFields.get(1).getText());
        } finally {
            if (copy != null) {
                Disposer.dispose(copy);
            }
            Disposer.dispose(source);
        }
    }

    public void testRsaListenerReplacementAndRemovalAreIdempotent() throws Exception {
        RS256Panel panel = new RS256Panel(getProject(), SignatureContext.EMPTY);
        try {
            AtomicInteger notifications = new AtomicInteger();
            panel.setSignatureContextChangedListener(context -> notifications.incrementAndGet());
            panel.setSignatureContextChangedListener(context -> notifications.incrementAndGet());

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            String publicKey = RsaUtils.toString((RSAPublicKey) generator.generateKeyPair().getPublic());
            EditorTextField publicField = findComponents(panel.getRoot(), EditorTextField.class).get(0);
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
}
