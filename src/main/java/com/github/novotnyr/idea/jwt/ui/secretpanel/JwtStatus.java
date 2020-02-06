package com.github.novotnyr.idea.jwt.ui.secretpanel;

public enum JwtStatus {
    NONE, VALID, MUTABLE;

    public boolean isValidatable() {
        return ordinal() >= VALID.ordinal();
    }
}
