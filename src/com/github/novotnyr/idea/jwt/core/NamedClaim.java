package com.github.novotnyr.idea.jwt.core;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamedClaim)) return false;
        NamedClaim<?> that = (NamedClaim<?>) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }
}
