package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import org.jspecify.annotations.NonNull;

import java.util.function.BooleanSupplier;

public class AddClaimActionButtonController implements AnActionButtonRunnable, AnActionButtonUpdater {

    private final DataTypeRegistry dataTypeRegistry = DataTypeRegistry.getInstance();

    private final BooleanSupplier canAddClaim;

    public AddClaimActionButtonController(BooleanSupplier canAddClaim) {
        this.canAddClaim = canAddClaim;
    }

    @Override
    public void run(AnActionButton button) {
        if (!this.canAddClaim.getAsBoolean()) {
            return;
        }
        createPopup().show(button.getPreferredPopupPoint());
    }

    DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (DataTypeRegistry.DataType dataType : this.dataTypeRegistry.getDataTypes()) {
            actionGroup.add(new NewClaimAction(dataType));
        }
        return actionGroup;
    }

    JBPopup createPopup() {
        return JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                createActionGroup(),
                DataContext.EMPTY_CONTEXT,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true);
    }

    public void onClaimTypeSelected(DataTypeRegistry.DataType dataType) {

    }

    @Override
    public boolean isEnabled(@NonNull AnActionEvent event) {
        return this.canAddClaim.getAsBoolean();
    }

    private class NewClaimAction extends AnAction {
        private final DataTypeRegistry.DataType dataType;

        private NewClaimAction(DataTypeRegistry.DataType dataType) {
            super("New " + dataType.toString().toLowerCase() + " claim");
            this.dataType = dataType;
        }

        @Override
        public void actionPerformed(@NonNull AnActionEvent event) {
            onClaimTypeSelected(this.dataType);
        }

        @Override
        public @NonNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }
}
