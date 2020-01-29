package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.BorderLayout;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class EncodedJwtPanel extends JPanel {
    private JTextArea encodedJwtTextArea = new JTextArea(8, 0);

    private JBScrollPane encodedJwtTextAreaScrollPane
            = new JBScrollPane(this.encodedJwtTextArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);

    private Highlighter.HighlightPainter headerPainter = new DefaultHighlighter.DefaultHighlightPainter(JBColor.LIGHT_GRAY);
    private Highlighter.HighlightPainter payloadPainter = new DefaultHighlighter.DefaultHighlightPainter(JBColor.LIGHT_GRAY);
    private Highlighter.HighlightPainter signaturePainter = new DefaultHighlighter.DefaultHighlightPainter(JBColor.GRAY);

    public EncodedJwtPanel() {
        super(new BorderLayout());

        add(this.encodedJwtTextAreaScrollPane, BorderLayout.CENTER);

        PromptSupport.init("Paste or type JWT here", null, null, this.encodedJwtTextArea);
        this.encodedJwtTextArea.setLineWrap(true);
        this.encodedJwtTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                try {
                    Document document = documentEvent.getDocument();
                    highlight(document);
                    String text = document.getText(0, document.getLength());
                    EncodedJwtPanel.this.firePropertyChange("jwt", "5679497a-18f9-43c2-bb6c-939574f51cb2", text);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void highlight(Document document) {
        try {
            String text = document.getText(0, document.getLength());
            String[] split = text.split("\\.");
            if(split.length != 3) {
                return;
            }
            String header = split[0];
            String payload = split[1];
            String signature = split[2];
            // TODO rework highlights
            //this.encodedJwtTextArea.getHighlighter().addHighlight(0, header.length(), this.headerPainter);
            this.encodedJwtTextArea.getHighlighter().addHighlight(header.length() + 1, header.length() + 1 + payload.length(), this.payloadPainter);
            // TODO rework highlights
            //this.encodedJwtTextArea.getHighlighter().addHighlight(header.length() + 1 + payload.length() + 1, header.length() + 1 + payload.length() + 1 + signature.length(), this.signaturePainter);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void setJwt(Jwt jwt) {
        this.encodedJwtTextArea.setText(jwt.toString());
    }

    public void reset() {
        this.encodedJwtTextArea.setText("");
    }
}
