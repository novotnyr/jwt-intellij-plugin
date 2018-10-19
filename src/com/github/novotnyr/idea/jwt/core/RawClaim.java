package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.TreeNode;

public class RawClaim extends NamedClaim<TreeNode> {

    public RawClaim(String name, TreeNode value) {
        super(name, value);
    }

    public RawClaim(String claimName, Claim claimValue) {
        this(claimName, Hacking.asJsonNode(claimValue));
    }

    @Override
    public RawClaim copy() {
        return new RawClaim(getName(), getValue());
    }
}
