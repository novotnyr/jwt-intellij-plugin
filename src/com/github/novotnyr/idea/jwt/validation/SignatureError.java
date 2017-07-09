package com.github.novotnyr.idea.jwt.validation;

public class SignatureError extends GlobalError {
    public SignatureError() {
        super("Signature failed");
    }

    private SignatureError(String message) {
        super(message);
    }

    public static SignatureError forEmptySecret() {
        return new SignatureError("Empty secret");
    }
}
