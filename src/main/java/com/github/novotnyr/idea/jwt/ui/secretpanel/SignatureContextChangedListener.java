package com.github.novotnyr.idea.jwt.ui.secretpanel;

import com.github.novotnyr.idea.jwt.SignatureContext;

public interface SignatureContextChangedListener {
    void onSignatureContextChanged(SignatureContext newSignatureContext);
}
