package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.SignatureAlgorithm;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.JwtFactory;
import com.github.novotnyr.idea.jwt.core.StringClaim;
import com.github.novotnyr.idea.jwt.hs256.HS256SignatureContext;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.TestActionEvent;

import java.util.Arrays;
import java.util.UUID;

import static com.intellij.ide.scratch.ScratchFileService.Option.create_if_missing;
import static org.junit.Assert.assertNotEquals;

public class CreateScratchFileActionTest extends LightPlatformTestCase {
    public void testFormatterTargetsScratchFileWhenAnotherEditorIsSelected() throws Exception {
        String otherText = "{\"untouched\":1}";
        String scratchText = "{\"outer\":{\"value\":2}}";
        VirtualFile otherFile = createScratch("other-", otherText);
        VirtualFile scratchFile = createScratch("target-", scratchText);
        FileEditorManager editors = FileEditorManager.getInstance(getProject());
        try {
            editors.openFile(otherFile, true);
            assertEquals(otherFile, editors.getSelectedFiles()[0]);

            new ScratchFileFormatter().reformat(getProject(), scratchFile);

            assertEquals(otherFile, editors.getSelectedFiles()[0]);
            assertEquals(otherText, textOf(otherFile));
            assertNotEquals(scratchText, textOf(scratchFile));
            assertTrue(textOf(scratchFile).contains("\"value\": 2"));
        } finally {
            closeAndDelete(otherFile, scratchFile);
        }
    }

    public void testActionOpensAndFormatsCreatedScratchFile() throws Exception {
        Jwt jwt = JwtFactory.getInstance().newJwt(SignatureAlgorithm.HS256,
                new HS256SignatureContext("test secret"), false);
        jwt.setPayloadClaim(new StringClaim("nonce", UUID.randomUUID().toString()));
        CreateScratchFileAction action = new CreateScratchFileAction() {
            @Override
            protected Jwt getJwt() {
                return jwt;
            }
        };
        String source = action.getJwtPayloadString();
        String expectedName = "jwt-" + source.hashCode() + ".json";
        FileEditorManager editors = FileEditorManager.getInstance(getProject());
        VirtualFile scratchFile = null;
        try {
            action.actionPerformed(TestActionEvent.createTestEvent(action,
                    SimpleDataContext.getProjectContext(getProject())));

            scratchFile = Arrays.stream(editors.getOpenFiles())
                    .filter(file -> expectedName.equals(file.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(scratchFile);
            assertTrue(Arrays.asList(editors.getSelectedFiles()).contains(scratchFile));
            assertNotEquals(source, textOf(scratchFile));
            assertTrue(textOf(scratchFile).contains("\"nonce\":"));
        } finally {
            closeAndDelete(scratchFile);
        }
    }

    private VirtualFile createScratch(String prefix, String text) {
        VirtualFile file = ScratchRootType.getInstance().createScratchFile(getProject(),
                prefix + UUID.randomUUID() + ".json", JsonLanguage.INSTANCE, text, create_if_missing);
        assertNotNull(file);
        return file;
    }

    private String textOf(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(psiFile);
        return psiFile.getText();
    }

    private void closeAndDelete(VirtualFile... files) throws Exception {
        FileEditorManager editors = FileEditorManager.getInstance(getProject());
        for (VirtualFile file : files) {
            if (file != null && file.isValid()) {
                editors.closeFile(file);
                WriteCommandAction.writeCommandAction(getProject()).run(() -> file.delete(this));
            }
        }
    }
}
