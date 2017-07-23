package com.github.novotnyr.idea.jwt.core;

public abstract class NamedClaim<T> {
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

    public String getValueString() {
        if(value == null) {
            return "null";
        } else {
            return this.value.toString();
        }
    }

    public abstract NamedClaim<T> copy();

    @Override
    public String toString() {
        return this.name + ":" + this.value;
    }
}
