package com.github.novotnyr.idea.jwt.core.ser;

import com.auth0.jwt.impl.ClaimsHolder;
import com.auth0.jwt.impl.PublicClaims;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class PayloadSerializer extends StdSerializer<ClaimsHolder> {

    public PayloadSerializer() {
        this(null);
    }

    private PayloadSerializer(Class<ClaimsHolder> t) {
        super(t);
    }

    @Override
    public void serialize(ClaimsHolder holder, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<Object, Object> safePayload = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : getClaims(holder).entrySet()) {
            switch (e.getKey()) {
                case PublicClaims.AUDIENCE:
                    if (e.getValue() instanceof String) {
                        safePayload.put(e.getKey(), e.getValue());
                        break;
                    }
                    String[] audArray = (String[]) e.getValue();
                    if (audArray.length == 1) {
                        safePayload.put(e.getKey(), audArray[0]);
                    } else if (audArray.length > 1) {
                        safePayload.put(e.getKey(), audArray);
                    }
                    break;
                case PublicClaims.EXPIRES_AT:
                case PublicClaims.ISSUED_AT:
                case PublicClaims.NOT_BEFORE:
                    safePayload.put(e.getKey(), dateToSeconds((Date) e.getValue()));
                    break;
                default:
                    if (e.getValue() instanceof Date) {
                        safePayload.put(e.getKey(), dateToSeconds((Date) e.getValue()));
                    } else {
                        safePayload.put(e.getKey(), e.getValue());
                    }
                    break;
            }
        }

        gen.writeObject(safePayload);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getClaims(ClaimsHolder holder) {
        try {
            Field claims = holder.getClass().getDeclaredField("claims");
            claims.setAccessible(true);
            return (Map<String, Object>) claims.get(holder);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private long dateToSeconds(Date date) {
        return date.getTime() / 1000;
    }
}
