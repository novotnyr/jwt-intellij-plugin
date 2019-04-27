package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.Constants;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButtonUpdater;

public class EditClaimActionButtonUpdater implements AnActionButtonUpdater {
    private boolean enabled = false;

    @Override
    public boolean isEnabled(AnActionEvent event) {
        Boolean data = event.getData(Constants.DataKeys.SECRET_IS_PRESENT);
        if(data != null) {
            this.enabled = data;
            return data;
        }
        return this.enabled;
    }
}
