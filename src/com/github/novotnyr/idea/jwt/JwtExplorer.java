package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.novotnyr.idea.jwt.ui.ClipboardUtils;
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;

public class JwtExplorer extends SimpleToolWindowPanel implements Disposable {

    private final EncodedJwtPanel encodedJwtPanel;
    private final JwtPanel jwtPanel;

    private DecodedJWT jwt;

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
                    JwtExplorer.this.jwt = JwtHelper.decodeHmac256(jwtString);
                    JwtExplorer.this.jwtPanel.setJwt(JwtExplorer.this.jwt);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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

        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(encodedJwtPanel);
        splitter.setSecondComponent(jwtPanel);

        setContent(splitter);
    }

    private void configureToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Copy as JSON", "Copy JWT to clipboard as JSON", AllIcons.FileTypes.Json) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                ClipboardUtils.copyPayload(JwtExplorer.this.jwt);
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
