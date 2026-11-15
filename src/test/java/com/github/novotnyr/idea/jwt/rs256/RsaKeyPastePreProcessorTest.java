package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContext;
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor;
import com.intellij.codeInsight.editorActions.PasteHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.EditorTextField;

import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class RsaKeyPastePreProcessorTest extends LightPlatformTestCase {
    public void testProducerPasteIsScopedToRsaKeyFields() {
        assertTrue(CopyPastePreProcessor.EP_NAME.getExtensionList().stream()
                .anyMatch(processor -> processor instanceof RsaKeyPastePreProcessor));

        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler originalHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_PASTE);
        RS256Panel panel = new RS256Panel(getProject(), SignatureContext.EMPTY);
        try {
            assertSame(originalHandler, actionManager.getActionHandler(IdeActions.ACTION_EDITOR_PASTE));

            List<EditorTextField> keyFields = new ArrayList<>();
            collectEditorTextFields(panel.getRoot(), keyFields);
            assertEquals(2, keyFields.size());

            String suppliedText = "-----BEGIN PUBLIC KEY-----\\nABC\\n-----END PUBLIC KEY-----";
            String expectedKeyText = "-----BEGIN PUBLIC KEY-----\nABC\n-----END PUBLIC KEY-----";
            for (EditorTextField keyField : keyFields) {
                pasteSuppliedText(keyField.getDocument(), suppliedText);
                assertEquals(expectedKeyText, keyField.getText());
            }

            EditorTextField unrelatedField = new EditorTextField(getProject(), FileTypes.PLAIN_TEXT);
            pasteSuppliedText(unrelatedField.getDocument(), suppliedText);
            assertEquals(suppliedText, unrelatedField.getText());
        } finally {
            Disposer.dispose(panel);
        }
    }

    private void pasteSuppliedText(Document document, String text) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Editor editor = editorFactory.createEditor(document, getProject());
        try {
            WriteCommandAction.runWriteCommandAction(getProject(), () ->
                    new PasteHandler(null).execute(editor, DataContext.EMPTY_CONTEXT, () -> new StringSelection(text)));
        } finally {
            editorFactory.releaseEditor(editor);
        }
    }

    private static void collectEditorTextFields(Component component, List<EditorTextField> fields) {
        if (component instanceof EditorTextField field) {
            fields.add(field);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectEditorTextFields(child, fields);
            }
        }
    }
}
