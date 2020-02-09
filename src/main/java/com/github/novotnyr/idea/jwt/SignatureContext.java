package com.github.novotnyr.idea.jwt;

public interface SignatureContext {
    SignatureContext EMPTY = () -> true;

    boolean isEmpty();
}
