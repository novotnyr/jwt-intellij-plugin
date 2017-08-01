package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.Constants;
import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AddClaimActionButtonController implements AnActionButtonRunnable, AnActionButtonUpdater {

    private DataTypeRegistry dataTypeRegistry = DataTypeRegistry.getInstance();

    private boolean enabled = false;

    @Override
    public void run(AnActionButton button) {
        Collection<DataTypeRegistry.DataType> dataTypes = this.dataTypeRegistry.getDataTypes();
        final JBList<String> list = new JBList<>(render(dataTypes));

        JBPopup popup = JBPopupFactory.getInstance()
                .createListPopupBuilder(list)
                .setItemChoosenCallback(new Runnable() {
                    @Override
                    public void run() {
                        onItemChosen(list);
                    }
                })
                .createPopup();

        final RelativePoint popupPoint = button.getPreferredPopupPoint();
        if (popupPoint != null) {
            popup.show(popupPoint);
        } else {
            JComponent contextComponent = button.getContextComponent();
            popup.showInCenterOf(contextComponent);
        }
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
        Boolean data = event.getData(Constants.DataKeys.SECRET_IS_PRESENT);
        if(data != null) {
            this.enabled = data;
            return data;
        }
        return this.enabled;
    }
}
