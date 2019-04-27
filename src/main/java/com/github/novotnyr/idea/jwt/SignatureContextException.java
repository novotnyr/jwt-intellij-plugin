package com.github.novotnyr.idea.jwt;

public class SignatureContextException extends RuntimeException {

    public SignatureContextException() {
        super();
    }

    public SignatureContextException(String msg) {
        super(msg);
    }

    public SignatureContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureContextException(Throwable cause) {
        super(cause);
    }
}