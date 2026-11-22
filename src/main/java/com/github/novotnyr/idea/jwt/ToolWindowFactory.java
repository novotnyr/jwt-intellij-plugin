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
        actionGroup.add(timestampFormatAction("Unix timestamp", Configuration.TimestampFormat.RAW));
        actionGroup.add(timestampFormatAction("ISO (UTC)", Configuration.TimestampFormat.ISO));
        actionGroup.add(timestampFormatAction("ISO (Local)", Configuration.TimestampFormat.ISO_LOCAL));
        actionGroup.add(timestampFormatAction("Medium Format", Configuration.TimestampFormat.MEDIUM));
        actionGroup.add(timestampFormatAction("Relative", Configuration.TimestampFormat.RELATIVE));

        DefaultActionGroup timestampFormatActionGroup = new DefaultActionGroup();
        timestampFormatActionGroup.add(actionGroup);
        return timestampFormatActionGroup;
    }

    private static CheckboxAction timestampFormatAction(String text, Configuration.TimestampFormat format) {
        return new CheckboxAction(text) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return Configuration.INSTANCE.getTimestampFormat() == format;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean state) {
                if (state) {
                    Configuration.INSTANCE.setTimestampFormat(format);
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
    }

}
