package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.Constants;
import com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButtonUpdater;

import static com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus.MUTABLE;

public class EditClaimActionButtonUpdater implements AnActionButtonUpdater {
    private boolean enabled = false;

    @Override
    public boolean isEnabled(AnActionEvent event) {
        JwtStatus jwtStatus = event.getData(Constants.DataKeys.JWT_STATUS);
        boolean isEditable = MUTABLE.equals(jwtStatus);
        if(isEditable) {
            this.enabled = isEditable;
            return isEditable;
        }
        return this.enabled;
    }
}
