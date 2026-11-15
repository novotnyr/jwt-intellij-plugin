package com.github.novotnyr.idea.jwt.ui.preferences;

import com.github.novotnyr.idea.jwt.SignatureContextException;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.LightPlatformTestCase;

import java.util.concurrent.atomic.AtomicReference;

public class PreferencesConfigurableTest extends LightPlatformTestCase {
    public void testApplyValidatesOnWorkerThread() throws ConfigurationException {
        AtomicReference<Thread> validationThread = new AtomicReference<>();
        PreferencesConfigurable configurable = new PreferencesConfigurable(getProject()) {
            @Override
            void validateKeyFiles(String publicKeyFile, String privateKeyFile) {
                validationThread.set(Thread.currentThread());
            }
        };
        try {
            configurable.createComponent();
            configurable.reset();
            configurable.apply();
            assertNotNull(validationThread.get());
            assertNotSame(Thread.currentThread(), validationThread.get());
        } finally {
            configurable.disposeUIResources();
        }
    }

    public void testApplyReportsValidationError() {
        PreferencesConfigurable configurable = new PreferencesConfigurable(getProject()) {
            @Override
            void validateKeyFiles(String publicKeyFile, String privateKeyFile) {
                throw new SignatureContextException("Invalid RSA key file");
            }
        };
        try {
            configurable.createComponent();
            configurable.reset();
            try {
                configurable.apply();
                fail("Expected a configuration error");
            } catch (ConfigurationException e) {
                assertEquals("Invalid RSA key file", e.getMessage());
            }
        } finally {
            configurable.disposeUIResources();
        }
    }
}
