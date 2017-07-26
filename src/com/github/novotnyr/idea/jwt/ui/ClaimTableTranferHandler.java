package com.github.novotnyr.idea.jwt.ui;

import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class ClaimTableTranferHandler extends TransferHandler {
    private static final String NO_PROPERTY = null;

    public ClaimTableTranferHandler() {
        super(NO_PROPERTY);
    }

    @Nullable
    @Override
    protected Transferable createTransferable(JComponent component) {
        if (!(component instanceof JTable)) {
            return null;
        }
        JTable table = (JTable) component;
        int selectedRow = table.getSelectedRow();
        if(selectedRow < 0) {
            return null;
        }
        Object claimName = table.getValueAt(selectedRow, 0);
        Object claimValue = table.getValueAt(selectedRow, 1);

        String data = claimName.toString() + "=" + claimValue.toString();
        return new StringSelection(data);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }
}
