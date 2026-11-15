package com.github.novotnyr.idea.jwt;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.UIUtil;

import javax.swing.text.Highlighter;

public class EncodedJwtPanelTest extends LightPlatformTestCase {
    public void testPayloadHighlightIsReplacedAndClearedAsTextChanges() {
        EncodedJwtPanel panel = new EncodedJwtPanel();
        JBTextArea textArea = UIUtil.findComponentOfType(panel, JBTextArea.class);
        assertNotNull(textArea);

        for (int i = 0; i < 5; i++) {
            String header = "header" + i;
            String payload = "payload" + i;
            textArea.setText(header + "." + payload + ".signature");
            assertPayloadHighlight(textArea, header.length() + 1, header.length() + 1 + payload.length());
        }

        textArea.setText("header.payload");
        assertEquals(0, textArea.getHighlighter().getHighlights().length);

        textArea.setText("header.payload.signature.extra");
        assertEquals(0, textArea.getHighlighter().getHighlights().length);

        textArea.setText("header.payload.signature.");
        assertEquals(0, textArea.getHighlighter().getHighlights().length);

        textArea.setText("a.b.c");
        assertPayloadHighlight(textArea, 2, 3);
        panel.reset();
        assertEquals(0, textArea.getHighlighter().getHighlights().length);
    }

    private void assertPayloadHighlight(JBTextArea textArea, int start, int end) {
        Highlighter.Highlight[] highlights = textArea.getHighlighter().getHighlights();
        assertEquals(1, highlights.length);
        assertEquals(start, highlights[0].getStartOffset());
        assertEquals(end, highlights[0].getEndOffset());
    }
}
