package com.github.novotnyr.idea.jwt;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JwtExplorer jwtExplorer = new JwtExplorer();
        jwtExplorer.setProject(project);

        ContentFactory contentFactory = getContentFactory();
        Content content = contentFactory.createContent(jwtExplorer, "", false);
        toolWindow.getContentManager().addContent(content);

        if(toolWindow instanceof ToolWindowEx) {
            ToolWindowEx extendedToolWindow = (ToolWindowEx) toolWindow;
            extendedToolWindow.setAdditionalGearActions(getToolWindowActionGroup());
        }

        content.setDisposer(jwtExplorer);
    }

    private static ContentFactory getContentFactory() {
        // IC-2022.1 introduced ContentFactory.getInstance() and deprecated ContentFactory.SERVICE.getInstance()
        // Use implementation from the IC sources until we increase the 'since' compatibility
        // https://github.com/JetBrains/intellij-community/blame/ca1151aa9f88e1e9f727d55736916cbe48d9518b/platform/ide-core/src/com/intellij/ui/content/ContentFactory.java#L27
        return ApplicationManager.getApplication().getService(ContentFactory.class);
    }

    private ActionGroup getToolWindowActionGroup() {
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
        });

        DefaultActionGroup timestampFormatActionGroup = new DefaultActionGroup();

        timestampFormatActionGroup.add(actionGroup);

        return timestampFormatActionGroup;
    }

}
