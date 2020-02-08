package com.github.novotnyr.idea.jwt.ui.preferences;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "jwt", storages = @Storage("jwt.xml"))
public class PluginPreferences implements PersistentStateComponent<PluginPreferences> {
    private String rs256PrivateKeyFile;

    private String rs256PublicKeyFile;

    public static PluginPreferences getInstance(Project project) {
        return ServiceManager.getService(project, PluginPreferences.class);
    }

    @Nullable
    @Override
    public PluginPreferences getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PluginPreferences persistedState) {
        XmlSerializerUtil.copyBean(persistedState, this);
    }

    public String getRs256PrivateKeyFile() {
        return rs256PrivateKeyFile;
    }

    public void setRs256PrivateKeyFile(String rs256PrivateKeyFile) {
        this.rs256PrivateKeyFile = rs256PrivateKeyFile;
    }

    public String getRs256PublicKeyFile() {
        return rs256PublicKeyFile;
    }

    public void setRs256PublicKeyFile(String rs256PublicKeyFile) {
        this.rs256PublicKeyFile = rs256PublicKeyFile;
    }
}
