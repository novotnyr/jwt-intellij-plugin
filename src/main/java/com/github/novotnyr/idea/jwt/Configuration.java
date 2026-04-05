package com.github.novotnyr.idea.jwt;

import java.util.List;

import static com.auth0.jwt.RegisteredClaims.*;

public enum Configuration {
    INSTANCE;

    private static final List<String> TIMESTAMP_DATE_FIELDS
            = List.of(EXPIRES_AT, NOT_BEFORE, ISSUED_AT);

    public enum TimestampFormat {
        RAW, ISO, RELATIVE
    }

    private TimestampFormat timestampFormat = TimestampFormat.ISO;

    public TimestampFormat getTimestampFormat() {
        return timestampFormat;
    }

    public void setTimestampFormat(TimestampFormat timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public List<String> getTimestampDateFields() {
        return TIMESTAMP_DATE_FIELDS;
    }
}
