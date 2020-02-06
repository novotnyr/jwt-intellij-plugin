package com.github.novotnyr.idea.jwt;


import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.ui.secretpanel.JwtStatus;
import com.intellij.openapi.actionSystem.DataKey;

public interface Constants {
    String CLAIMS_TABLE_NAME = "claimsTable";

    interface DataKeys {
        DataKey<Boolean> SECRET_IS_PRESENT = DataKey.create("secretIsPresent");

        DataKey<JwtStatus> JWT_STATUS = DataKey.create("jwtStatus");

        DataKey<String> SECRET = DataKey.create("secret");

        DataKey<Jwt> JWT = DataKey.create("jwt");
    }
}
