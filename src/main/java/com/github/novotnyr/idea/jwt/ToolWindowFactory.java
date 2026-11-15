package com.github.novotnyr.idea.jwt;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

public class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JwtExplorer jwtExplorer = new JwtExplorer(project);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(jwtExplorer, "", false);
        contentManager.addContent(content);

        if(toolWindow instanceof ToolWindowEx) {
            ToolWindowEx extendedToolWindow = (ToolWindowEx) toolWindow;
            extendedToolWindow.setAdditionalGearActions(getToolWindowActionGroup());
        }

        content.setDisposer(jwtExplorer);
    }

    ActionGroup getToolWindowActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("Timestamp format", true);
        actionGroup.add(new CheckboxAction("Unix timestamp") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RAW;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean state) {
                Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RAW);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        actionGroup.add(new CheckboxAction("ISO") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.ISO;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean state) {
                Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.ISO);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        actionGroup.add(new CheckboxAction("Relative") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RELATIVE;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean state) {
                Configuration.INSTANCE.setTimestampFormat(Configuration.TimestampFormat.RELATIVE);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        DefaultActionGroup timestampFormatActionGroup = new DefaultActionGroup();

        timestampFormatActionGroup.add(actionGroup);

        return timestampFormatActionGroup;
    }

}
