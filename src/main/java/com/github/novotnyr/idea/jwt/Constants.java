package com.github.novotnyr.idea.jwt;


import com.intellij.openapi.actionSystem.DataKey;

public interface Constants {
    String CLAIMS_TABLE_NAME = "claimsTable";

    interface DataKeys {
        DataKey<String> SECRET = DataKey.create("secret");
    }
}
