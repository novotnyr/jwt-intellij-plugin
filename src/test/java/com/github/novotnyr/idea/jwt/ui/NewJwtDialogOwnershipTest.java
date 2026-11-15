package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.JwtPanel;
import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.hs256.HS256Panel;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.github.novotnyr.idea.jwt.hs384.HS384Panel;
import com.github.novotnyr.idea.jwt.hs384.HS384SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RS256Panel;
import com.github.novotnyr.idea.jwt.rs256.RS256SignatureContext;
import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.github.novotnyr.idea.jwt.ui.secretpanel.SecretPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.EditorTextField;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class NewJwtDialogOwnershipTest extends LightPlatformTestCase {
    public void testOkWithMissingSecretKeepsDialogOpen() {
        TestDialog dialog = new TestDialog(getProject(), algorithm -> null);
        try {
            dialog.accept();
            assertFalse(dialog.isDisposed());
        } finally {
            dialog.doCancelAction();
        }
    }

    public void testCancelLeavesToolWindowSecretAndComponentUntouched() {
        JwtPanel owner = new JwtPanel(getProject());
        try {
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            HS256Panel ownerSecret = (HS256Panel) owner.getSecretPanel();
            ownerSecret.setSecret("window secret");
            Container ownerParent = ownerSecret.getRoot().getParent();

            AtomicReference<SecretPanel> dialogPanel = new AtomicReference<>();
            TestDialog dialog = new TestDialog(getProject(), algorithm -> {
                SecretPanel copy = owner.copySecretPanelForDialog(algorithm);
                dialogPanel.set(copy);
                return copy;
            });
            try {
                assertEquals(SignatureAlgorithm.HS256, dialog.selectedAlgorithm());
                JTextField dialogSecret = findComponents(dialog.center(), JTextField.class).get(0);
                assertEquals("window secret", dialogSecret.getText());
                JTextField ownerField = findComponents(ownerSecret.getRoot(), JTextField.class).get(0);
                assertNotSame(ownerField.getDocument(), dialogSecret.getDocument());

                dialogSecret.setText("discarded draft");
                dialog.doCancelAction();

                assertTrue(dialog.isDisposed());
                assertTrue(Disposer.isDisposed(dialogPanel.get()));
                assertSame(ownerParent, ownerSecret.getRoot().getParent());
                assertEquals("window secret", ((HS256SignatureContext) ownerSecret.getSignatureContext()).getSecret());
            } finally {
                if (!dialog.isDisposed()) {
                    dialog.doCancelAction();
                }
            }
        } finally {
            Disposer.dispose(owner);
        }
    }

    public void testOkTransfersOnlyTheSelectedDialogValue() {
        JwtPanel owner = new JwtPanel(getProject());
        try {
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            HS256Panel ownerSecret = (HS256Panel) owner.getSecretPanel();
            ownerSecret.setSecret("window secret");

            TestDialog dialog = new TestDialog(getProject(), owner::copySecretPanelForDialog);
            try {
                findComponents(dialog.center(), JTextField.class).get(0).setText("accepted secret");
                assertEquals("window secret", ((HS256SignatureContext) ownerSecret.getSignatureContext()).getSecret());

                dialog.accept();
                assertTrue(dialog.isDisposed());
                Jwt acceptedJwt = dialog.getJwt();
                SignatureContext acceptedContext = dialog.getSignatureContext();
                assertEquals(SignatureAlgorithm.HS256, acceptedJwt.getAlgorithm());
                assertEquals("accepted secret", ((HS256SignatureContext) acceptedContext).getSecret());

                owner.setJwt(acceptedJwt);
                owner.setSignatureContext(acceptedContext);
                assertSame(ownerSecret, owner.getSecretPanel());
                assertEquals("accepted secret", ((HS256SignatureContext) ownerSecret.getSignatureContext()).getSecret());
            } finally {
                if (!dialog.isDisposed()) {
                    dialog.doCancelAction();
                }
            }
        } finally {
            Disposer.dispose(owner);
        }
    }

    public void testRsaDraftIsCopiedAsRawTextAndCancelDoesNotMoveIt() {
        JwtPanel owner = new JwtPanel(getProject());
        try {
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel ownerSecret = (RS256Panel) owner.getSecretPanel();
            ownerSecret.removeSignatureContextChangedListener();
            List<EditorTextField> ownerFields = findComponents(ownerSecret.getRoot(), EditorTextField.class);
            ownerFields.get(0).setText("unfinished public key");
            ownerFields.get(1).setText("unfinished private key");

            TestDialog dialog = new TestDialog(getProject(), owner::copySecretPanelForDialog);
            try {
                dialog.selectAlgorithm(SignatureAlgorithm.RS256);
                List<EditorTextField> dialogFields = findComponents(dialog.center(), EditorTextField.class);
                assertEquals(2, dialogFields.size());
                assertNotSame(ownerFields.get(0).getDocument(), dialogFields.get(0).getDocument());
                assertEquals("unfinished public key", dialogFields.get(0).getText());
                assertEquals("unfinished private key", dialogFields.get(1).getText());

                dialog.doCancelAction();
                assertEquals("unfinished public key", ownerFields.get(0).getText());
                assertEquals("unfinished private key", ownerFields.get(1).getText());
            } finally {
                if (!dialog.isDisposed()) {
                    dialog.doCancelAction();
                }
            }
        } finally {
            Disposer.dispose(owner);
        }
    }

    public void testOkChangesOnlyTheSelectedAlgorithmSecret() {
        JwtPanel owner = new JwtPanel(getProject());
        try {
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
            HS256Panel hs256 = (HS256Panel) owner.getSecretPanel();
            hs256.setSecret("hs256 original");
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS384));
            HS384Panel hs384 = (HS384Panel) owner.getSecretPanel();
            hs384.setSecret("hs384 original");

            TestDialog dialog = new TestDialog(getProject(), owner::copySecretPanelForDialog);
            try {
                assertEquals(SignatureAlgorithm.HS256, dialog.selectedAlgorithm());
                dialog.selectAlgorithm(SignatureAlgorithm.HS384);
                JTextField dialogSecret = findComponents(dialog.center(), JTextField.class).get(0);
                assertEquals("hs384 original", dialogSecret.getText());
                dialogSecret.setText("hs384 accepted");
                dialog.accept();

                owner.setJwt(dialog.getJwt());
                owner.setSignatureContext(dialog.getSignatureContext());
                assertSame(hs384, owner.getSecretPanel());
                assertEquals("hs384 accepted", ((HS384SignatureContext) hs384.getSignatureContext()).getSecret());

                owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.HS256));
                assertSame(hs256, owner.getSecretPanel());
                assertEquals("hs256 original", ((HS256SignatureContext) hs256.getSignatureContext()).getSecret());
            } finally {
                if (!dialog.isDisposed()) {
                    dialog.doCancelAction();
                }
            }
        } finally {
            Disposer.dispose(owner);
        }
    }

    public void testRsaAcceptedValueSurvivesDialogDisposal() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        RS256SignatureContext context = new RS256SignatureContext.Builder()
                .withPublicKey(RsaUtils.toString((RSAPublicKey) pair.getPublic()))
                .withPrivateKey(RsaUtils.toString((RSAPrivateKey) pair.getPrivate()))
                .build();

        JwtPanel owner = new JwtPanel(getProject());
        try {
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            owner.setSignatureContext(context);
            AtomicReference<SecretPanel> rsaDialogPanel = new AtomicReference<>();
            TestDialog dialog = new TestDialog(getProject(), algorithm -> {
                SecretPanel copy = owner.copySecretPanelForDialog(algorithm);
                if (SignatureAlgorithm.RS256.equals(algorithm)) {
                    rsaDialogPanel.set(copy);
                }
                return copy;
            });
            try {
                dialog.selectAlgorithm(SignatureAlgorithm.RS256);
                dialog.accept();
                assertTrue(dialog.isDisposed());
                assertTrue(Disposer.isDisposed(rsaDialogPanel.get()));
                assertEquals(SignatureAlgorithm.RS256, dialog.getJwt().getAlgorithm());
                RS256SignatureContext accepted = (RS256SignatureContext) dialog.getSignatureContext();
                assertEquals(context.getPublicKey(), accepted.getPublicKey());
                assertEquals(context.getPrivateKey(), accepted.getPrivateKey());
            } finally {
                if (!dialog.isDisposed()) {
                    dialog.doCancelAction();
                }
            }
        } finally {
            Disposer.dispose(owner);
        }
    }

    private static Jwt jwtForAlgorithm(String algorithm) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"alg\":\"" + algorithm + "\"}").getBytes(StandardCharsets.UTF_8));
        return new Jwt(header + ".e30.AQ");
    }

    private static <T extends Component> List<T> findComponents(Component root, Class<T> type) {
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

    private static class TestDialog extends NewJwtDialog {
        TestDialog(Project project, Function<String, SecretPanel> copyProvider) {
            super(project, copyProvider);
        }

        JComponent center() {
            return createCenterPanel();
        }

        String selectedAlgorithm() {
            return (String) findComponents(center(), JComboBox.class).get(0).getSelectedItem();
        }

        void selectAlgorithm(String algorithm) {
            findComponents(center(), JComboBox.class).get(0).setSelectedItem(algorithm);
        }

        void accept() {
            doOKAction();
        }
    }
}
