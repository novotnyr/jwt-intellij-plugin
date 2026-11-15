package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.components.JBList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        Collection<DataTypeRegistry.DataType> dataTypes = this.dataTypeRegistry.getDataTypes();
        final JBList<String> list = new JBList<>(render(dataTypes));

        JBPopup popup = new PopupChooserBuilder<>(list)
                .setItemChosenCallback(s -> onItemChosen(list))
                .createPopup();

        popup.show(button.getPreferredPopupPoint());
    }

    private List<String> render(Collection<DataTypeRegistry.DataType> dataTypes) {
        List<String> result = new ArrayList<>();
        for (DataTypeRegistry.DataType dataType : dataTypes) {
            result.add("New " + dataType.toString().toLowerCase() + " claim");
        }
        return result;
    }

    private void onItemChosen(JBList<String> list) {
        int selectedIndex = list.getSelectedIndex();
        DataTypeRegistry.DataType dataType = this.dataTypeRegistry.getDataTypes().get(selectedIndex);
        onClaimTypeSelected(dataType);
    }

    public void onClaimTypeSelected(DataTypeRegistry.DataType dataType) {

    }

    @Override
    public boolean isEnabled(AnActionEvent event) {
        return this.canAddClaim.getAsBoolean();
    }
}
