package com.github.novotnyr.idea.jwt.action;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;

final class ScratchFileFormatter {
    void reformat(Project project, VirtualFile scratchFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(scratchFile);
        if (psiFile == null) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, "Format JWT scratch file", null,
                () -> CodeStyleManager.getInstance(project).reformat(psiFile), psiFile);
    }
}
