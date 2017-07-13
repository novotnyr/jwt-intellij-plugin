package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JwtClaimsTableModel extends AbstractTableModel implements TableCellRenderer {
    private DecodedJWT jwt;

    private DefaultTableCellRenderer delegateRenderer = new DefaultTableCellRenderer();

    private List<ClaimError> claimErrors = new ArrayList<>();

    private Color originalForegroundColor;

    public JwtClaimsTableModel(DecodedJWT jwt) {
        this.jwt = jwt;

        this.originalForegroundColor = delegateRenderer.getForeground();
    }

    @Override
    public int getRowCount() {
        return jwt.getClaims().size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Map<String, Claim> claims = jwt.getClaims();
        Iterator<String> iter = claims.keySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            String key = iter.next();
            if(i == rowIndex) {
                switch (columnIndex) {
                    case 0:
                        return key;
                    case 1:
                        return ClaimUtils.get(key, claims.get(key));
                }
            }
            i++;
        }
        return null;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel renderer = (JLabel) this.delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        renderer.setForeground(this.originalForegroundColor);

        Iterator<String> iter = jwt.getClaims().keySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            String key = iter.next();
            if(row == i) {
                for (ClaimError claimError : this.claimErrors) {
                    if(claimError.getClaim().equalsIgnoreCase(key)) {
                        TextAttributes attributes = EditorColorsManager.getInstance()
                                .getGlobalScheme()
                                .getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES);
                        renderer.setForeground(attributes.getEffectColor());
                        return renderer;
                    }
                }
            }
            i++;
        }
        return renderer;
    }

    public void setClaimErrors(List<ClaimError> claimErrors) {
        this.claimErrors = claimErrors;
    }
}

