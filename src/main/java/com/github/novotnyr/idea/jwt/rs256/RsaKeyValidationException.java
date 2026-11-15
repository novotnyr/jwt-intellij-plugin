package com.github.novotnyr.idea.jwt.rs256;

import com.github.novotnyr.idea.jwt.SignatureContextException;

public final class RsaKeyValidationException extends SignatureContextException {
    public enum Field {
        PUBLIC,
        PRIVATE
    }

    private final Field field;

    public RsaKeyValidationException(Field field, SignatureContextException cause) {
        super(cause.getMessage(), cause);
        this.field = field;
    }

    public Field getField() {
        return this.field;
    }
}
