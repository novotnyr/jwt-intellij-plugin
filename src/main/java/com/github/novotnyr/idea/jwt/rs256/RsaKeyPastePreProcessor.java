package com.github.novotnyr.idea.jwt.rs256;

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RawText;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RsaKeyPastePreProcessor implements CopyPastePreProcessor {
    private static final Key<Boolean> RSA_KEY_DOCUMENT = Key.create("com.github.novotnyr.jwt-intellij-plugin.rsaKeyDocument");

    static void mark(Document document) {
        document.putUserData(RSA_KEY_DOCUMENT, Boolean.TRUE);
    }

    @Override
    public @Nullable String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
        return text;
    }

    @Override
    public @NotNull String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
        return Boolean.TRUE.equals(editor.getDocument().getUserData(RSA_KEY_DOCUMENT))
                ? RsaUtils.sanitizeWhitespace(text)
                : text;
    }
}
