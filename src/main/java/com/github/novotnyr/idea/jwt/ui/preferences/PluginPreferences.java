package com.github.novotnyr.idea.jwt.ui.preferences;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@State(name = "jwt", storages = @Storage("jwt.xml"))
public class PluginPreferences implements PersistentStateComponent<PluginPreferences> {
    private String rs256PrivateKeyFile;

    private String rs256PublicKeyFile;

    private final CopyOnWriteArrayList<Runnable> keyFilesChangeListeners = new CopyOnWriteArrayList<>();

    public static PluginPreferences getInstance(Project project) {
        return project.getService(PluginPreferences.class);
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
        return this.rs256PrivateKeyFile;
    }

    public void setRs256PrivateKeyFile(String rs256PrivateKeyFile) {
        this.rs256PrivateKeyFile = rs256PrivateKeyFile;
    }

    public String getRs256PublicKeyFile() {
        return this.rs256PublicKeyFile;
    }

    public void setRs256PublicKeyFile(String rs256PublicKeyFile) {
        this.rs256PublicKeyFile = rs256PublicKeyFile;
    }

    public void addKeyFilesChangeListener(Runnable listener, Disposable parent) {
        this.keyFilesChangeListeners.add(listener);
        Disposer.register(parent, () -> this.keyFilesChangeListeners.remove(listener));
    }

    public void updateKeyFiles(String publicKeyFile, String privateKeyFile) {
        if (Objects.equals(this.rs256PublicKeyFile, publicKeyFile)
                && Objects.equals(this.rs256PrivateKeyFile, privateKeyFile)) {
            return;
        }
        this.rs256PublicKeyFile = publicKeyFile;
        this.rs256PrivateKeyFile = privateKeyFile;
        this.keyFilesChangeListeners.forEach(Runnable::run);
    }
}
