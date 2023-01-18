package com.github.novotnyr.idea.jwt.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class Icons {
    public static Icon createScratchFile() {
        return IconLoader.getIcon("/icons/openNewTab.svg", Icons.class);
    }

    public static Icon export() {
        Icon icon = IconLoader.getIcon("/actions/export.png", Icons.class);
        //noinspection ConstantConditions
        if (icon != null) {
            return icon;
        }
        return load("/toolbarDecorator/export.svg");
    }

    // Supports IJ 2020.1+
    private static Icon load(String path) {
        try {
            Class<?> iconManagerClass = Class.forName("com.intellij.ui.IconManager");
            Method getInstanceMethod = iconManagerClass.getMethod("getInstance", String.class, Class.class);
            return (Icon) getInstanceMethod.invoke(null, path, AllIcons.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            return null;
        }
    }

}
