package com.github.novotnyr.idea.jwt.validation;

public class GlobalError {
    private final String message;

    public GlobalError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return this.message;
    }
}
