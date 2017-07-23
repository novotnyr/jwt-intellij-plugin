package com.github.novotnyr.idea.jwt.core;

public class NamedClaim<T> {
    private final String name;

    private final T value;

    public NamedClaim(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }
}
