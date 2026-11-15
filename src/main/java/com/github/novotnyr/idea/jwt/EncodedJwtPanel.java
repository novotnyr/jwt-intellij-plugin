package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.core.Jwt;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.BorderLayout;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class EncodedJwtPanel extends JPanel {
    private final JBTextArea encodedJwtTextArea = new JBTextArea(8, 0);

    private final JBScrollPane encodedJwtTextAreaScrollPane
            = new JBScrollPane(this.encodedJwtTextArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);

    private final Highlighter.HighlightPainter payloadPainter = new DefaultHighlighter.DefaultHighlightPainter(JBColor.LIGHT_GRAY);

    /** Tag for the payload highlight owned by this panel. */
    private Object payloadHighlightTag;

    public EncodedJwtPanel() {
        super(new BorderLayout());

        add(this.encodedJwtTextAreaScrollPane, BorderLayout.CENTER);

        this.encodedJwtTextArea.getEmptyText().setText("Paste or type JWT here");
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
        Highlighter highlighter = this.encodedJwtTextArea.getHighlighter();
        if (this.payloadHighlightTag != null) {
            highlighter.removeHighlight(this.payloadHighlightTag);
            this.payloadHighlightTag = null;
        }

        try {
            String text = document.getText(0, document.getLength());
            String[] segments = text.split("\\.", -1);
            if (segments.length != 3) {
                return;
            }

            int payloadStart = segments[0].length() + 1;
            int payloadEnd = payloadStart + segments[1].length();
            this.payloadHighlightTag = highlighter.addHighlight(payloadStart, payloadEnd, this.payloadPainter);
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
