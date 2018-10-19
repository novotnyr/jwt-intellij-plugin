package com.github.novotnyr.idea.jwt.core;

import com.fasterxml.jackson.core.TreeNode;

public class RawClaim extends NamedClaim<TreeNode> {

    public RawClaim(String name, TreeNode value) {
        super(name, value);
    }

    @Override
    public RawClaim copy() {
        return new RawClaim(getName(), getValue());
    }
}
