package com.github.novotnyr.idea.jwt.validation;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record JwtErrors(List<ClaimError> claimErrors, @Nullable SignatureError signatureError) {
    public JwtErrors {
        claimErrors = List.copyOf(claimErrors);
    }

    /**
     * Returns whether a signature error was recorded.
     * Interpret {@code false} as a valid signature only when verification was performed.
     */
    public boolean hasSignatureError() {
        return this.signatureError != null;
    }
}
