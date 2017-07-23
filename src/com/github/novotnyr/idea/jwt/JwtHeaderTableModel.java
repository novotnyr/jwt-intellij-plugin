package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class JwtHeaderTableModel extends AbstractTableModel {
    private Jwt jwt;

    public JwtHeaderTableModel(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    public int getRowCount() {
        return this.jwt.getHeaderClaims().size();
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
        List<NamedClaim<?>> headerClaims = this.jwt.getHeaderClaims();
        NamedClaim<?> claim = headerClaims.get(rowIndex);
        if(columnIndex == 0) {
            return claim.getName();
        } else if(columnIndex == 1) {
            return claim.getValue();
        }
        return "N/A";
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }
}

