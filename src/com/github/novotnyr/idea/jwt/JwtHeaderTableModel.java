package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;

import javax.swing.table.AbstractTableModel;
import java.util.Map;
import java.util.TreeMap;

public class JwtHeaderTableModel extends AbstractTableModel {
    private Map<String, String> jwtMap = new TreeMap<String, String>();

    public JwtHeaderTableModel(DecodedJWT jwt) {
        process(jwt);
    }

    private void process(DecodedJWT jwt) {
        putIfNotNull("alg", jwt.getAlgorithm());
        putIfNotNull("typ", jwt.getType());
        putIfNotNull("cty", jwt.getContentType());
        putIfNotNull("kid", jwt.getKeyId());
    }

    @Override
    public int getRowCount() {
        return this.jwtMap.size();
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
        int i = 0;
        for (Map.Entry<String, String> entry : this.jwtMap.entrySet()) {
            if(rowIndex == i) {
                if(columnIndex == 0) {
                    return entry.getKey();
                } else {
                    return entry.getValue();
                }
            }
            i++;
        }
        return "N/A";
    }

    private void putIfNotNull(String key, String value) {
        if(value != null) {
            this.jwtMap.put(key, value);
        }
    }

    public void setJwt(DecodedJWT jwt) {
        process(jwt);
    }
}

