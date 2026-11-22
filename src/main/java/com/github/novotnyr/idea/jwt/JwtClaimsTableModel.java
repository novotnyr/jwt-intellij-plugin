package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import com.github.novotnyr.idea.jwt.validation.ClaimError;
import org.jetbrains.annotations.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

import javax.swing.table.AbstractTableModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JwtClaimsTableModel extends AbstractTableModel {
    private final Jwt jwt;

    private List<ClaimError> claimErrors = new ArrayList<>();

    public JwtClaimsTableModel(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    public int getRowCount() {
        return this.jwt.getPayloadClaims().size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Claim";
            case 1 -> "Value";
            default -> "-";
        };
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
                Instant instant = NumericDate.toInstant(dateClaim.getValue());
                return instant == null ? dateClaim.getValueString() : instant.toString();
            }
            Date date = dateClaim.getDate();
            if (date != null && Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RELATIVE) {
                PrettyTime prettyTime = new PrettyTime();
                return prettyTime.format(date);
            }
            if (Configuration.INSTANCE.getTimestampFormat() == Configuration.TimestampFormat.RAW) {
                return dateClaim.getValue();
            }
            return dateClaim.getValueString();
        }
        return claim.getValue();
    }

    public NamedClaim<?> getClaimAt(int rowIndex) {
        return this.jwt.getPayloadClaims().get(rowIndex);
    }

    @Nullable
    ClaimError getClaimErrorAt(int modelRow) {
        List<NamedClaim<?>> payloadClaims = this.jwt.getPayloadClaims();
        if (modelRow < 0 || modelRow >= payloadClaims.size()) {
            return null;
        }

        String claimName = payloadClaims.get(modelRow).getName();
        for (ClaimError claimError : this.claimErrors) {
            if (claimError.getClaim().equals(claimName)) {
                return claimError;
            }
        }
        return null;
    }

    public void setClaimErrors(List<ClaimError> claimErrors) {
        this.claimErrors = List.copyOf(claimErrors);
        if (getRowCount() > 0) {
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }
}

