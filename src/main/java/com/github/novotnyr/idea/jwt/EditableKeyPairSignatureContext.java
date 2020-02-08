package com.github.novotnyr.idea.jwt;

public class EditableKeyPairSignatureContext implements SignatureContext {
    public static final EditableKeyPairSignatureContext INSTANCE = new EditableKeyPairSignatureContext();

    private EditableKeyPairSignatureContext() {
        // empty constructor
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
