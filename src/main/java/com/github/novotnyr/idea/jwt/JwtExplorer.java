package com.github.novotnyr.idea.jwt;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.github.novotnyr.idea.jwt.action.CreateScratchFileAction;
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;

public class JwtExplorer extends SimpleToolWindowPanel implements Disposable {

    private final EncodedJwtPanel encodedJwtPanel;
    private final JwtPanel jwtPanel;

    private Jwt jwt = Jwt.EMPTY;

    public JwtExplorer() {
        super(true);
        configureToolbar();

        this.encodedJwtPanel = new EncodedJwtPanel();
        this.jwtPanel = new JwtPanel();
        this.encodedJwtPanel.addPropertyChangeListener("jwt", event -> {
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
                        .show(RelativePoint.getNorthWestOf(JwtExplorer.this.encodedJwtPanel),
                                Balloon.Position.atRight);
            } catch (JWTDecodeException e) {
                JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder("JWT has a wrong syntax", MessageType.ERROR, null)
                        .setFadeoutTime(7500)
                        .createBalloon()
                        .show(RelativePoint.getNorthWestOf(JwtExplorer.this.encodedJwtPanel),
                                Balloon.Position.atRight);
            } catch (SecretNotSpecifiedException e) {
                JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder(e.getMessage(), MessageType.ERROR, null)
                        .setFadeoutTime(7500)
                        .createBalloon()
                        .show(RelativePoint.getNorthWestOf(JwtExplorer.this.jwtPanel.getSecretPanel().getBaloonableComponent()),
                                Balloon.Position.atRight);
            }
        });
        this.jwtPanel.addJwtListener(evt -> {
            Jwt jwt = (Jwt) evt.getNewValue();
            JwtExplorer.this.encodedJwtPanel.setJwt(jwt);
        });

        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(this.encodedJwtPanel);
        splitter.setSecondComponent(this.jwtPanel.getRootPanel());

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
                    JwtExplorer.this.jwtPanel.setSignatureContext(dialog.getSignatureContext());
                    JwtExplorer.this.jwtPanel.setJwt(jwt);
                }
            }
        });

        group.add(new AnAction("Copy payload as JSON", "Copy JWT payload to clipboard as JSON", AllIcons.FileTypes.Json) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                Jwt jwt = JwtExplorer.this.jwt;
                if (jwt != null) {
                    ClipboardUtils.copyPayload(JwtExplorer.this.jwt);
                }
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(!JwtExplorer.this.jwt.isEmpty());
            }
        });
        group.add(new CreateScratchFileAction() {
            @Override
            protected Jwt getJwt() {
                return JwtExplorer.this.jwt;
            }
        });

        group.add(new AnAction("Reset", "Reset All Fields", AllIcons.General.Reset) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                JwtExplorer.this.jwt = Jwt.EMPTY;
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

    public void setProject(Project project) {
        this.jwtPanel.setProject(project);
    }
}
