package com.github.novotnyr.idea.jwt.core;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PayloadClaims implements Iterable<NamedClaim<?>> {
    private List<NamedClaim<?>> claims = new LinkedList<>();

    public void add(NamedClaim<?> claim) {
        this.claims.add(claim);
    }

    @Override
    public @NonNull Iterator<NamedClaim<?>> iterator() {
        return Collections.unmodifiableList(this.claims).iterator();
    }
}
