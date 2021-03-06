package com.github.novotnyr.idea.jwt.ui;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.JwtHelper;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.util.ui.TextTransferable;

public class ClipboardUtils {
    public static void copyPayload(DecodedJWT jwt) {
        TextTransferable textTransferable = new TextTransferable(JwtHelper.prettyUnbase64Json(jwt.getPayload()));
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }

    public static void copyPayload(Jwt jwt) {
        TextTransferable textTransferable = new TextTransferable(JwtHelper.prettyUnbase64Json(jwt.getPayloadString()));
        CopyPasteManagerEx.getInstanceEx().setContents(textTransferable);
    }
}
