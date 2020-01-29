package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.JwtHelper;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.intellij.icons.AllIcons;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import static com.intellij.ide.scratch.ScratchFileService.Option.create_if_missing;

public abstract class CreateScratchFileAction extends AnAction {
    public static final String EXTENSION = "json";

    private String extension = EXTENSION;

    public CreateScratchFileAction() {
        super("Create scratch file from payload", "Create scratch file from payload", AllIcons.Actions.ShowViewer);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        String jwtPayloadString = getJwtPayloadString();

        String fileName = "jwt-" + jwtPayloadString.hashCode() + "." + this.extension;

        VirtualFile scratchVirtualFile = ScratchRootType.getInstance()
                .createScratchFile(project, fileName, JsonLanguage.INSTANCE, jwtPayloadString, create_if_missing);
        if (scratchVirtualFile != null) {
            FileEditorManager.getInstance(project).openFile(scratchVirtualFile, true);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Jwt jwt = getJwt();
        e.getPresentation().setEnabled(jwt != null && !jwt.isEmpty());
    }


    protected abstract Jwt getJwt();

    protected String getJwtPayloadString() {
        Jwt jwt = getJwt();
        if (jwt == null) {
            return null;
        }
        return JwtHelper.prettyUnbase64Json(jwt.getPayloadString());
    }
}
