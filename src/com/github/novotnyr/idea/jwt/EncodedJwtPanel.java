package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.intellij.ui.DocumentAdapter;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.BorderLayout;
import java.awt.Color;

public class EncodedJwtPanel extends JPanel {
    private JTextArea encodedJwtTextArea = new JTextArea(8, 0);

    private Highlighter.HighlightPainter headerPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
    private Highlighter.HighlightPainter payloadPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
    private Highlighter.HighlightPainter signaturePainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY);

    public EncodedJwtPanel() {
        super(new BorderLayout());

        add(this.encodedJwtTextArea, BorderLayout.CENTER);

        PromptSupport.init("Paste or type JWT here", null, null, this.encodedJwtTextArea);
        this.encodedJwtTextArea.setLineWrap(true);
        this.encodedJwtTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                try {
                    Document document = documentEvent.getDocument();
                    highlight(document);
                    String text = document.getText(0, document.getLength());
                    EncodedJwtPanel.this.firePropertyChange("jwt", "", text);
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
