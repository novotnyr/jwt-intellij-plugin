package com.github.novotnyr.idea.jwt.validation;

public class UnknownAlgorithmException extends RuntimeException {
    public UnknownAlgorithmException(String algorithm) {
        super("Unknown algorithm " + algorithm);
    }
}
