package com.github.novotnyr.idea.jwt.ui;

/**
 * Indicates that a result was requested before the New JWT dialog was accepted.
 */
public final class NewJwtDialogNotAcceptedException extends IllegalStateException {
    public NewJwtDialogNotAcceptedException() {
        super("New JWT dialog has not been accepted");
    }
}
