package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.ocpsoft.prettytime.PrettyTime;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class JwtClaimsTableModel extends AbstractTableModel implements TableCellRenderer {
    private Jwt jwt;

    private DefaultTableCellRenderer errorTableCellRenderer = new DefaultTableCellRenderer();

    private DefaultTableCellRenderer defaultDelegateRenderer = new DefaultTableCellRenderer();

    private List<ClaimError> claimErrors = new ArrayList<>();

    public JwtClaimsTableModel(Jwt jwt) {
        this.jwt = jwt;

        TextAttributes attributes = EditorColorsManager.getInstance()
                .getGlobalScheme()
                .getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES);
        this.errorTableCellRenderer.setForeground(attributes.getEffectColor());
    }

    @Override
    public int getRowCount() {
        return jwt.getPayloadClaims().size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Claim";
            case 1:
                return "Value";
            default:
                return "-";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        List<NamedClaim<?>> claims = this.jwt.getPayloadClaims();
        for (int i = 0; i < claims.size(); i++) {
            NamedClaim<?> claim = claims.get(i);
            if(i == rowIndex) {
                switch (columnIndex) {
                    case 0:
                        return claim.getName();
                    case 1:
                        return render(claim);
                }
            }
        }
        return null;
    }

    private Object render(NamedClaim<?> claim) {
        if(claim instanceof DateClaim) {
            DateClaim dateClaim = (DateClaim) claim;
            if (Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.ISO) {
                return dateClaim.getValue();
            }
            if (Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RELATIVE) {
                PrettyTime prettyTime = new PrettyTime();
                return prettyTime.format(dateClaim.getValue());
            }
            if (Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RAW) {
                return dateClaim.getValue().getTime() / 1000;
            }
        }
        return claim.getValue();
    }

    public NamedClaim<?> getClaimAt(int rowIndex) {
        return jwt.getPayloadClaims().get(rowIndex);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        List<NamedClaim<?>> payloadClaims = jwt.getPayloadClaims();
        for (int i = 0; i < payloadClaims.size(); i++) {
            NamedClaim<?> claim = payloadClaims.get(i);

            if(row == i) {
                for (ClaimError claimError : this.claimErrors) {
                    if(claimError.getClaim().equalsIgnoreCase(claim.getName())) {
                        return this.errorTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    }
                }
            }
        }
        return this.defaultDelegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public void setClaimErrors(List<ClaimError> claimErrors) {
        this.claimErrors = claimErrors;
    }
}

