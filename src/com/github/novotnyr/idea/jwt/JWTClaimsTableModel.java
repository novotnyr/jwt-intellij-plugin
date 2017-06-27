package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.swing.table.AbstractTableModel;
import java.util.Iterator;
import java.util.Map;

public class JWTClaimsTableModel extends AbstractTableModel {
    private DecodedJWT jwt;

    public JWTClaimsTableModel(DecodedJWT jwt) {
        this.jwt = jwt;
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
}
