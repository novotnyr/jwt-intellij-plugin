package com.github.novotnyr.idea.jwt.ui;

import com.intellij.ui.PopupMenuListenerAdapter;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import java.awt.Point;

public class UiUtils {
    public static void configureTableRowSelectionOnPopup(final JPopupMenu popupMenu) {
        popupMenu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JTable table = (JTable) popupMenu.getInvoker();
                        int rowAtPoint = table.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), table));
                        if (rowAtPoint > -1) {
                            table.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                        }
                    }
                });
            }
        });
    }
}
