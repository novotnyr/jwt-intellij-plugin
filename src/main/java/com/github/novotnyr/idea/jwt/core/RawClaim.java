package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

public class RawClaim extends NamedClaim<TreeNode> {

    public RawClaim(String name, TreeNode value) {
        super(name, value);
    }

    public RawClaim(String claimName, Claim claimValue) {
        this(claimName, claimValue.isNull() ? NullNode.getInstance() : claimValue.as(JsonNode.class));
    }

    @Override
    public RawClaim copy() {
        return new RawClaim(getName(), getValue());
    }
}
