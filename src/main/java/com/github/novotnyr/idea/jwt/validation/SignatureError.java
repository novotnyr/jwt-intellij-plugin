package com.github.novotnyr.idea.jwt.validation;

public class SignatureError {
    private final String message;

    public SignatureError() {
        this("Signature failed");
    }

    public SignatureError(String message) {
        this.message = message;
    }

    public static SignatureError forEmptySecret() {
        return new SignatureError("Empty secret");
    }

    public static SignatureError forUnknownAlgorithm(String algorithmName) {
        return new SignatureError("Unsupported algorithm " + algorithmName);
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return this.message;
    }
}
