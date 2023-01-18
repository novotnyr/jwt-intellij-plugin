package com.github.novotnyr.idea.jwt.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public abstract class Icons {
    public static Icon createScratchFile() {
        return IconLoader.getIcon("/icons/openNewTab.svg", Icons.class);
    }
}
