package com.github.novotnyr.idea.jwt.ui;

import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.ui.DocumentAdapter;

import javax.swing.event.DocumentEvent;

public class DelegatingDocumentListener<T> extends DocumentAdapter implements DocumentListener {
    private final T delegate;

    public DelegatingDocumentListener(T delegate) {
        this.delegate = delegate;
    }

    @Override
    protected final void textChanged(DocumentEvent event) {
        onDocumentChanged();
    }

    @Override
    public final void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
        onDocumentChanged();
    }

    protected void onDocumentChanged() {
        // implement by subclass
    }
}