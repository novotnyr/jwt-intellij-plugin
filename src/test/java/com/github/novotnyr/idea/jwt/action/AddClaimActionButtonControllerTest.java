package com.github.novotnyr.idea.jwt.action;

import com.github.novotnyr.idea.jwt.datatype.DataTypeRegistry;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.TestActionEvent;

public class AddClaimActionButtonControllerTest extends LightPlatformTestCase {
    public void testActionGroupEntriesDispatchTheirClaimTypes() {
        RecordingController controller = new RecordingController();
        DefaultActionGroup actionGroup = controller.createActionGroup();
        AnAction[] actions = actionGroup.getChildren((AnActionEvent) null);
        DataTypeRegistry.DataType[] dataTypes = DataTypeRegistry.DataType.values();

        assertEquals(dataTypes.length, actions.length);
        for (int i = 0; i < dataTypes.length; i++) {
            assertEquals("New " + dataTypes[i].toString().toLowerCase() + " claim",
                    actions[i].getTemplatePresentation().getText());

            actions[i].actionPerformed(TestActionEvent.createTestEvent(actions[i]));

            assertEquals(dataTypes[i], controller.selectedType);
        }
    }

    public void testPopupUsesNativeActionGroupPopupRenderer() {
        JBPopup popup = new RecordingController().createPopup();
        try {
            assertEquals("ActionGroupPopup", popup.getClass().getSimpleName());
        } finally {
            Disposer.dispose(popup);
        }
    }

    private static class RecordingController extends AddClaimActionButtonController {
        private DataTypeRegistry.DataType selectedType;

        private RecordingController() {
            super(() -> true);
        }

        @Override
        public void onClaimTypeSelected(DataTypeRegistry.DataType dataType) {
            this.selectedType = dataType;
        }
    }
}