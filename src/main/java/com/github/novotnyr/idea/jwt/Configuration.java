package com.github.novotnyr.idea.jwt;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.auth0.jwt.RegisteredClaims.*;

public enum Configuration {
    INSTANCE;

    private static final List<String> TIMESTAMP_DATE_FIELDS
            = List.of(EXPIRES_AT, NOT_BEFORE, ISSUED_AT);

    public enum TimestampFormat {
        RAW, ISO, ISO_LOCAL, RELATIVE, MEDIUM
    }

    // Read and write on the Swing event dispatch thread (EDT).
    // Volatile provides visibility if accessed off the EDT, but does not make the update and listener notification atomic.
    private volatile TimestampFormat timestampFormat = TimestampFormat.ISO;

    private final List<Runnable> timestampFormatChangeListeners = new CopyOnWriteArrayList<>();

    public TimestampFormat getTimestampFormat() {
        return this.timestampFormat;
    }

    public void setTimestampFormat(TimestampFormat timestampFormat) {
        if (this.timestampFormat == timestampFormat) {
            return;
        }
        this.timestampFormat = timestampFormat;
        this.timestampFormatChangeListeners.forEach(Runnable::run);
    }

    void addTimestampFormatChangeListener(Runnable listener, Disposable parent) {
        this.timestampFormatChangeListeners.add(listener);
        Disposer.register(parent, () -> this.timestampFormatChangeListeners.remove(listener));
    }

    public List<String> getTimestampDateFields() {
        return TIMESTAMP_DATE_FIELDS;
    }
}
