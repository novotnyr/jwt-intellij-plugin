package com.github.novotnyr.idea.jwt.validation;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record JwtErrors(List<ClaimError> claimErrors, @Nullable SignatureError signatureError) {
    public JwtErrors(List<ClaimError> claimErrors, @Nullable SignatureError signatureError) {
        this.claimErrors = List.copyOf(claimErrors);
        this.signatureError = signatureError;
    }

    public boolean hasSignatureError() {
        return this.signatureError != null;
    }
}
