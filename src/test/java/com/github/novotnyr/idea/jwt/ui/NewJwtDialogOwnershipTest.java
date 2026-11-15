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
import com.github.novotnyr.idea.jwt.ui.preferences.PluginPreferences;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.UIUtil;
import com.intellij.openapi.ui.ValidationInfo;
import org.jspecify.annotations.NonNull;

import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

public class NewJwtDialogOwnershipTest extends LightPlatformTestCase {
    public void testOkWithMissingSecretKeepsDialogOpen() {
        TestDialog dialog = new TestDialog(getProject());
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

            TestDialog dialog = new TestDialog(getProject());
            try {
                assertEquals(SignatureAlgorithm.HS256, dialog.selectedAlgorithm());
                JTextField dialogSecret = findComponents(dialog.center(), JTextField.class).getFirst();
                assertEquals("", dialogSecret.getText());
                JTextField ownerField = findComponents(ownerSecret.getRoot(), JTextField.class).getFirst();
                assertNotSame(ownerField.getDocument(), dialogSecret.getDocument());

                dialogSecret.setText("discarded draft");
                dialog.doCancelAction();

                assertTrue(dialog.isDisposed());
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

            TestDialog dialog = new TestDialog(getProject());
            try {
                JTextField dialogSecret = findComponents(dialog.center(), JTextField.class).getFirst();
                assertEquals("", dialogSecret.getText());
                dialogSecret.setText("accepted secret");
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

    public void testRsaDialogDoesNotUseTheToolWindowDraft() {
        PluginPreferences preferences = PluginPreferences.getInstance(getProject());
        String oldPublic = preferences.getRs256PublicKeyFile();
        String oldPrivate = preferences.getRs256PrivateKeyFile();
        preferences.setRs256PublicKeyFile(null);
        preferences.setRs256PrivateKeyFile(null);
        JwtPanel owner = new JwtPanel(getProject());
        try {
            owner.setJwt(jwtForAlgorithm(SignatureAlgorithm.RS256));
            RS256Panel ownerSecret = (RS256Panel) owner.getSecretPanel();
            ownerSecret.removeSignatureContextChangedListener();
            List<EditorTextField> ownerFields = findComponents(ownerSecret.getRoot(), EditorTextField.class);
            ownerFields.get(0).setText("unfinished public key");
            ownerFields.get(1).setText("unfinished private key");

            ManualExecutor executor = new ManualExecutor();
            TestDialog dialog = new TestDialog(getProject(), executor);
            try {
                selectRsaWithDefaults(dialog, executor);
                List<EditorTextField> dialogFields = findComponents(dialog.center(), EditorTextField.class);
                assertEquals(2, dialogFields.size());
                assertNotSame(ownerFields.getFirst().getDocument(), dialogFields.get(0).getDocument());
                assertEquals("", dialogFields.get(0).getText());
                assertEquals("", dialogFields.get(1).getText());

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
            preferences.setRs256PublicKeyFile(oldPublic);
            preferences.setRs256PrivateKeyFile(oldPrivate);
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

            TestDialog dialog = new TestDialog(getProject());
            try {
                assertEquals(SignatureAlgorithm.HS256, dialog.selectedAlgorithm());
                dialog.selectAlgorithm(SignatureAlgorithm.HS384);
                JTextField dialogSecret = findComponents(dialog.center(), JTextField.class).getFirst();
                assertEquals("", dialogSecret.getText());
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

    public void testRsaPreferenceKeysAreAcceptedAndSurviveDialogDisposal() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        String publicKey = RsaUtils.toString((RSAPublicKey) pair.getPublic());
        String privateKey = RsaUtils.toString((RSAPrivateKey) pair.getPrivate());
        Path publicFile = Files.createTempFile("jwt-dialog-public-", ".pem");
        Path privateFile = Files.createTempFile("jwt-dialog-private-", ".pem");
        Files.writeString(publicFile, publicKey);
        Files.writeString(privateFile, privateKey);
        PluginPreferences preferences = PluginPreferences.getInstance(getProject());
        String oldPublic = preferences.getRs256PublicKeyFile();
        String oldPrivate = preferences.getRs256PrivateKeyFile();
        preferences.setRs256PublicKeyFile(publicFile.toString());
        preferences.setRs256PrivateKeyFile(privateFile.toString());
        try {
            ManualExecutor executor = new ManualExecutor();
            TestDialog dialog = new TestDialog(getProject(), executor);
            try {
                selectRsaWithDefaults(dialog, executor);
                List<EditorTextField> fields = findComponents(dialog.center(), EditorTextField.class);
                assertEquals(publicKey, fields.get(0).getText());
                assertEquals(privateKey, fields.get(1).getText());

                dialog.accept();
                executor.runNext();
                UIUtil.dispatchAllInvocationEvents();
                assertTrue(dialog.isDisposed());
                assertEquals(SignatureAlgorithm.RS256, dialog.getJwt().getAlgorithm());
                RS256SignatureContext accepted = (RS256SignatureContext) dialog.getSignatureContext();
                assertEquals(pair.getPublic(), accepted.getPublicKey());
                assertEquals(pair.getPrivate(), accepted.getPrivateKey());
            } finally {
                if (!dialog.isDisposed()) {
                    dialog.doCancelAction();
                }
            }
        } finally {
            preferences.setRs256PublicKeyFile(oldPublic);
            preferences.setRs256PrivateKeyFile(oldPrivate);
            Files.deleteIfExists(publicFile);
            Files.deleteIfExists(privateFile);
        }
    }

    public void testMalformedRsaPublicKeyStaysOpenAndCanBeCorrected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        String publicKey = RsaUtils.toString((RSAPublicKey) pair.getPublic());
        String privateKey = RsaUtils.toString((RSAPrivateKey) pair.getPrivate());
        ManualExecutor executor = new ManualExecutor();
        TestDialog dialog = new TestDialog(getProject(), executor);
        try {
            selectRsaWithDefaults(dialog, executor);
            List<EditorTextField> fields = findComponents(dialog.center(), EditorTextField.class);
            fields.get(0).setText("not a public key");
            fields.get(1).setText(privateKey);

            dialog.accept();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            ValidationInfo error = dialog.validationInfo();
            assertNotNull(error);
            assertSame(fields.getFirst(), error.component);
            assertFalse(dialog.isDisposed());

            fields.getFirst().setText(publicKey);
            dialog.accept();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertTrue(dialog.isDisposed());
            assertEquals(SignatureAlgorithm.RS256, dialog.getJwt().getAlgorithm());
        } finally {
            if (!dialog.isDisposed()) {
                dialog.doCancelAction();
            }
        }
    }

    public void testWrongPrivateKeyTypeReportsPrivateField() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        String publicKey = RsaUtils.toString((RSAPublicKey) pair.getPublic());
        ManualExecutor executor = new ManualExecutor();
        TestDialog dialog = new TestDialog(getProject(), executor);
        try {
            selectRsaWithDefaults(dialog, executor);
            List<EditorTextField> fields = findComponents(dialog.center(), EditorTextField.class);
            fields.get(0).setText(publicKey);
            fields.get(1).setText(publicKey);

            dialog.accept();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();

            ValidationInfo error = dialog.validationInfo();
            assertNotNull(error);
            assertSame(fields.get(1), error.component);
            assertFalse(dialog.isDisposed());
        } finally {
            dialog.doCancelAction();
        }
    }

    public void testIatChangeDiscardsPendingRsaResult() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        ManualExecutor executor = new ManualExecutor();
        TestDialog dialog = new TestDialog(getProject(), executor);
        try {
            selectRsaWithDefaults(dialog, executor);
            List<EditorTextField> fields = findComponents(dialog.center(), EditorTextField.class);
            fields.get(0).setText(RsaUtils.toString((RSAPublicKey) pair.getPublic()));
            fields.get(1).setText(RsaUtils.toString((RSAPrivateKey) pair.getPrivate()));

            dialog.accept();
            JCheckBox addIat = findComponents(dialog.center(), JCheckBox.class).getFirst();
            addIat.setSelected(!addIat.isSelected());
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertFalse(dialog.isDisposed());

            dialog.accept();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertTrue(dialog.isDisposed());
        } finally {
            if (!dialog.isDisposed()) {
                dialog.doCancelAction();
            }
        }
    }

    public void testRsaValidationResultIsDiscardedAfterEditOrCancel() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        ManualExecutor executor = new ManualExecutor();
        TestDialog dialog = new TestDialog(getProject(), executor);
        try {
            selectRsaWithDefaults(dialog, executor);
            List<EditorTextField> fields = findComponents(dialog.center(), EditorTextField.class);
            fields.get(0).setText(RsaUtils.toString((RSAPublicKey) pair.getPublic()));
            fields.get(1).setText(RsaUtils.toString((RSAPrivateKey) pair.getPrivate()));

            dialog.accept();
            fields.get(0).setText("edited while checking");
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertFalse(dialog.isDisposed());

            fields.getFirst().setText(RsaUtils.toString((RSAPublicKey) pair.getPublic()));
            dialog.accept();
            dialog.doCancelAction();
            executor.runNext();
            UIUtil.dispatchAllInvocationEvents();
            assertTrue(dialog.isDisposed());
        } finally {
            if (!dialog.isDisposed()) {
                dialog.doCancelAction();
            }
        }
    }

    public void testDelayedDialogDefaultsDoNotOverwriteTypedKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        Path publicFile = Files.createTempFile("jwt-dialog-public-", ".pem");
        Path privateFile = Files.createTempFile("jwt-dialog-private-", ".pem");
        Files.writeString(publicFile, RsaUtils.toString((RSAPublicKey) pair.getPublic()));
        Files.writeString(privateFile, RsaUtils.toString((RSAPrivateKey) pair.getPrivate()));
        PluginPreferences preferences = PluginPreferences.getInstance(getProject());
        String oldPublic = preferences.getRs256PublicKeyFile();
        String oldPrivate = preferences.getRs256PrivateKeyFile();
        preferences.setRs256PublicKeyFile(publicFile.toString());
        preferences.setRs256PrivateKeyFile(privateFile.toString());
        try {
            ManualExecutor executor = new ManualExecutor();
            TestDialog dialog = new TestDialog(getProject(), executor);
            try {
                dialog.selectAlgorithm(SignatureAlgorithm.RS256);
                List<EditorTextField> fields = findComponents(dialog.center(), EditorTextField.class);
                fields.getFirst().setText("my public key draft");
                executor.runNext();
                UIUtil.dispatchAllInvocationEvents();
                assertEquals("my public key draft", fields.get(0).getText());
                assertEquals("", fields.get(1).getText());
            } finally {
                dialog.doCancelAction();
            }
        } finally {
            preferences.setRs256PublicKeyFile(oldPublic);
            preferences.setRs256PrivateKeyFile(oldPrivate);
            Files.deleteIfExists(publicFile);
            Files.deleteIfExists(privateFile);
        }
    }

    private static Jwt jwtForAlgorithm(String algorithm) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"alg\":\"" + algorithm + "\"}").getBytes(StandardCharsets.UTF_8));
        return new Jwt(header + ".e30.AQ");
    }

    private static void selectRsaWithDefaults(TestDialog dialog, ManualExecutor executor) {
        dialog.selectAlgorithm(SignatureAlgorithm.RS256);
        executor.runNext();
        UIUtil.dispatchAllInvocationEvents();
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
        TestDialog(Project project) {
            super(project);
        }

        TestDialog(Project project, Executor executor) {
            super(project, executor);
        }

        JComponent center() {
            return createCenterPanel();
        }

        String selectedAlgorithm() {
            return (String) findComponents(center(), JComboBox.class).getFirst().getSelectedItem();
        }

        void selectAlgorithm(String algorithm) {
            findComponents(center(), JComboBox.class).getFirst().setSelectedItem(algorithm);
        }

        void accept() {
            doOKAction();
        }

        ValidationInfo validationInfo() {
            return doValidate();
        }
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
