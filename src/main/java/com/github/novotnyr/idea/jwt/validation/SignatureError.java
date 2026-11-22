package com.github.novotnyr.idea.jwt.validation;

import org.jetbrains.annotations.NotNull;

public record SignatureError(String message) {
    public SignatureError() {
        this("Signature verification failed");
    }

    public static SignatureError forEmptySecret() {
        return new SignatureError("Empty secret");
    }

    public static SignatureError forUnknownAlgorithm(String algorithmName) {
        return new SignatureError("Unsupported algorithm " + algorithmName);
    }

    @Override
    @NotNull
    public String toString() {
        return String.valueOf(this.message);
    }
}
