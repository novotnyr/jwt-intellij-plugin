package com.github.novotnyr.idea.jwt;


import com.github.novotnyr.idea.jwt.core.Jwt;
import com.intellij.openapi.actionSystem.DataKey;

public interface Constants {
    String CLAIMS_TABLE_NAME = "claimsTable";

    interface DataKeys {
        DataKey<Boolean> SECRET_IS_PRESENT = DataKey.create("secretIsPresent");

        DataKey<String> SECRET = DataKey.create("secret");

        DataKey<Jwt> JWT = DataKey.create("jwt");
    }
}
