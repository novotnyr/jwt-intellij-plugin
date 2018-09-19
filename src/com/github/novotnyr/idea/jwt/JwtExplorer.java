package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.github.novotnyr.idea.jwt.core.Jwt;
import com.github.novotnyr.idea.jwt.ui.ClipboardUtils;
import com.github.novotnyr.idea.jwt.ui.NewJwtDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class JwtExplorer extends SimpleToolWindowPanel implements Disposable {

    private final EncodedJwtPanel encodedJwtPanel;
    private final JwtPanel jwtPanel;

    private Jwt jwt;

    public JwtExplorer() {
        super(true);
        configureToolbar();

        encodedJwtPanel = new EncodedJwtPanel();
        jwtPanel = new JwtPanel();
        encodedJwtPanel.addPropertyChangeListener("jwt", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                try {
                    String jwtString = (String) event.getNewValue();
                    Jwt jwt = StringUtils.isEmpty(jwtString) ? Jwt.EMPTY : new Jwt(jwtString);
                    JwtExplorer.this.jwt = jwt;
                    JwtExplorer.this.jwtPanel.setJwt(jwt);
                } catch (SignatureVerificationException e) {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(e.getMessage(), MessageType.ERROR, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(encodedJwtPanel),
                                    Balloon.Position.atRight);
                } catch (@SuppressWarnings("TryWithIdenticalCatches") JWTDecodeException e) {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("JWT has a wrong syntax", MessageType.ERROR, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(encodedJwtPanel),
                                    Balloon.Position.atRight);
                } catch (SecretNotSpecifiedException e) {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(e.getMessage(), MessageType.ERROR, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(jwtPanel.getSecretTextField()),
                                    Balloon.Position.atRight);
                }
            }
        });
        jwtPanel.addPropertyChangeListener("jwt", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Jwt jwt = (Jwt) evt.getNewValue();
                encodedJwtPanel.setJwt(jwt);
            }
        });

        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(encodedJwtPanel);
        splitter.setSecondComponent(jwtPanel);

        setContent(splitter);
    }

    private void configureToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("New JWT", "Create a new JWT", AllIcons.FileTypes.Any_type) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                NewJwtDialog dialog = new NewJwtDialog(anActionEvent.getProject());
                if (dialog.showAndGet()) {
                    Jwt jwt = dialog.getJwt();
                    JwtExplorer.this.jwt = jwt;
                    JwtExplorer.this.encodedJwtPanel.setJwt(jwt);
                    JwtExplorer.this.jwtPanel.setSigningCredentials(dialog.getSigningCredentials());
                    JwtExplorer.this.jwtPanel.setJwt(jwt);
                }
            }
        });

        group.add(new AnAction("Copy as JSON", "Copy JWT to clipboard as JSON", AllIcons.FileTypes.Json) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                Jwt jwt = JwtExplorer.this.jwt;
                if (jwt != null) {
                    ClipboardUtils.copyPayload(JwtExplorer.this.jwt);
                }
            }
        });
        group.add(new AnAction("Reset", "Reset All Fields", AllIcons.General.Reset) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                JwtExplorer.this.jwt = null;
                JwtExplorer.this.encodedJwtPanel.reset();
                JwtExplorer.this.jwtPanel.reset();
            }
        });

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("jwtToolbar", group, true);
        actionToolBar.setTargetComponent(this.encodedJwtPanel);

        setToolbar(JBUI.Panels.simplePanel(actionToolBar.getComponent()));
    }


    @Override
    public void dispose() {

    }
}
