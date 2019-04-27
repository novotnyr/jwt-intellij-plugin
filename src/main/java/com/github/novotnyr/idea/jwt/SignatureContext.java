package com.github.novotnyr.idea.jwt;

public interface SignatureContext {
    SignatureContext EMPTY = new SignatureContext() {
        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    boolean isEmpty();
}
