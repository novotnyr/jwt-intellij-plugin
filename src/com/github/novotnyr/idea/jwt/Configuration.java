package com.github.novotnyr.idea.jwt;

public enum Configuration {
    INSTANCE;

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
}
