package com.github.novotnyr.idea.jwt.core.ser;

import com.auth0.jwt.RegisteredClaims;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.novotnyr.idea.jwt.core.DateClaim;
import com.github.novotnyr.idea.jwt.core.NamedClaim;
import com.github.novotnyr.idea.jwt.core.PayloadClaims;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class PayloadClaimsSerializer extends StdSerializer<PayloadClaims> {

    public PayloadClaimsSerializer() {
        super(PayloadClaims.class);
    }

    @Override
    public void serialize(PayloadClaims payloadClaims, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<Object, Object> safePayload = new LinkedHashMap<>();
        for (NamedClaim<?> claim : payloadClaims) {
            var name = claim.getName();
            switch(claim.getName()) {
                case RegisteredClaims.AUDIENCE:
                    if (claim.getValue() instanceof String value) {
                        safePayload.put(name, value);
                        break;
                    }
                    String[] audArray = (String[]) claim.getValue();
                    if (audArray.length == 1) {
                        safePayload.put(name, audArray[0]);
                    } else if (audArray.length > 1) {
                        safePayload.put(name, audArray);
                    }
                    break;
                case RegisteredClaims.EXPIRES_AT:
                case RegisteredClaims.ISSUED_AT:
                case RegisteredClaims.NOT_BEFORE:
                    if (claim instanceof DateClaim dateClaim) {
                        safePayload.put(name, dateClaim.getTimestamp());
                        break;
                    } else {
                        safePayload.put(name, claim.getValueString());
                    }
                default:
                    safePayload.put(name, getValue(claim));
                    break;
            }
        }

        gen.writeObject(safePayload);
    }

    private Object getValue(NamedClaim<?> claim) {
        if (claim.getValue() instanceof Date date) {
            return date.getTime() / 1000;
        } else {
            return claim.getValueString();
        }
    }
}
