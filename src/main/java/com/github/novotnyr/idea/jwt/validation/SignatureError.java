package com.github.novotnyr.idea.jwt.validation;

public class SignatureError extends GlobalError {
    public SignatureError() {
        super("Signature failed");
    }

    public SignatureError(String message) {
        super(message);
    }

    public static SignatureError forEmptySecret() {
        return new SignatureError("Empty secret");
    }

    public static SignatureError forUnknownAlgorithm(String algorithmName) {
        return new SignatureError("Unsupported algorithm " + algorithmName);
    }
}
