package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.awt.RelativePoint;

import java.io.UnsupportedEncodingException;

public class JwtExplorer extends SimpleToolWindowPanel implements Disposable {
    public JwtExplorer() {
        super(true);

        EncodedJwtPanel encodedJwtPanel = new EncodedJwtPanel();
        JwtPanel jwtPanel = new JwtPanel();
        encodedJwtPanel.addPropertyChangeListener("jwt", event -> {
            try {
                String jwtString = (String) event.getNewValue();
                DecodedJWT jwt = JwtHelper.decodeHmac256(jwtString);
                jwtPanel.setJwt(jwt);
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
        });

        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(encodedJwtPanel);
        splitter.setSecondComponent(jwtPanel);

        setContent(splitter);
    }


    @Override
    public void dispose() {

    }
}
