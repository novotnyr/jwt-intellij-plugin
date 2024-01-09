package com.github.novotnyr.idea.jwt.ui.editor;

import com.intellij.codeInsight.actions.FileInEditorProcessor;
import com.intellij.codeInsight.actions.LastRunReformatCodeOptionsProvider;
import com.intellij.codeInsight.actions.ReformatCodeRunOptions;
import com.intellij.codeInsight.actions.TextRangeType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class EditorReformatter {

    public static final String FILE_IN_EDITOR_PROCESSOR_CLASS_NAME = "com.intellij.codeInsight.actions.FileInEditorProcessor";

    @SuppressWarnings("MissingRecentApi")
    public void reformatActiveEditor(Project project, VirtualFile virtualFile) {
        if (!isFileInEditorProcessorAvailable()) {
            return;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return;
        }
        LastRunReformatCodeOptionsProvider provider = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
        ReformatCodeRunOptions currentRunOptions = provider.getLastRunOptions(psiFile);

        TextRangeType processingScope = TextRangeType.WHOLE_FILE;
        currentRunOptions.setProcessingScope(processingScope);
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor == null) {
            return;
        }
        new FileInEditorProcessor(psiFile, selectedTextEditor, currentRunOptions).processCode();
    }

    private boolean isFileInEditorProcessorAvailable() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(FILE_IN_EDITOR_PROCESSOR_CLASS_NAME, false, classLoader);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
