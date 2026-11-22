package com.github.novotnyr.idea.jwt;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;

final class ClaimErrorValueRenderer extends JPanel implements TableCellRenderer {
    private final JLabel errorIcon = new JLabel();

    ClaimErrorValueRenderer() {
        super(new BorderLayout(JBUI.scale(4), 0));
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        removeAll();
        setBorder(null);

        TableCellRenderer delegate = table.getDefaultRenderer(value == null ? Object.class : value.getClass());
        Component valueComponent = delegate.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        if (valueComponent instanceof JComponent component) {
            setBackground(component.getBackground());
            Border valueBorder = component.getBorder();
            setBorder(valueBorder);
            component.setBorder(null);
        } else {
            setBackground(table.getBackground());
        }
        add(valueComponent, BorderLayout.CENTER);

        boolean hasError = table.getModel() instanceof JwtClaimsTableModel model
                && model.getClaimErrorAt(table.convertRowIndexToModel(row)) != null;
        this.errorIcon.setIcon(hasError ? AllIcons.General.BalloonError : null);
        this.errorIcon.setVisible(hasError);
        add(this.errorIcon, BorderLayout.EAST);
        return this;
    }
}
