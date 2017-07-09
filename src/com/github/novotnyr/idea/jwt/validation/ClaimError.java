package com.github.novotnyr.idea.jwt.validation;

public class ClaimError {
    private final String claim;

    private final String message;

    public ClaimError(String claim, String message) {
        this.claim = claim;
        this.message = message;
    }

    public String getClaim() {
        return claim;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return this.claim + ": " + this.message;
    }
}
