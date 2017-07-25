package com.github.novotnyr.idea.jwt;

import java.util.ArrayList;
import java.util.List;

public enum Configuration {
    INSTANCE;

    public enum TimestampFormat {
        RAW, ISO, RELATIVE
    }

    private TimestampFormat timestampFormat = TimestampFormat.ISO;

    private List<String> timestampDateFields = new ArrayList<String>() {{
        add("exp");
        add("nbf");
        add("iat");
    }};

    public TimestampFormat getTimestampFormat() {
        return timestampFormat;
    }

    public void setTimestampFormat(TimestampFormat timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public List<String> getTimestampDateFields() {
        return timestampDateFields;
    }

    public void setTimestampDateFields(List<String> timestampDateFields) {
        this.timestampDateFields = timestampDateFields;
    }

}
