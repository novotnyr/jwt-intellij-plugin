package com.github.novotnyr.idea.jwt;

public class SecretNotSpecifiedException extends RuntimeException {
    public SecretNotSpecifiedException() {
        super("Secret must be specified");
    }
}