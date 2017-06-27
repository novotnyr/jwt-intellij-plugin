package com.github.novotnyr.idea.jwt;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JwtExplorer jwtExplorer = new JwtExplorer();

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(jwtExplorer, "", false);
        toolWindow.getContentManager().addContent(content);

        Disposer.register(project, jwtExplorer);
    }

}
