package com.github.novotnyr.idea.jwt;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;

public class TextHighlightPainter implements Highlighter.HighlightPainter {
    private static final Stroke STROKE = new BasicStroke(1.0F, 0, 0, 1.0F, new float[]{1.0F}, 0.0F);

    public void paint(Graphics graphics, int p0, int p1, Shape bounds, JTextComponent textComponent) {
        try {
            Rectangle target = textComponent.getUI()
                    .getRootView(textComponent)
                    .modelToView(p0, Position.Bias.Forward, p1, Position.Bias.Backward, bounds)
                    .getBounds();
            Graphics2D g2d = (Graphics2D) graphics.create();

            try {
                g2d.setStroke(STROKE);
                g2d.setColor(textComponent.getSelectionColor());
            } finally {
                g2d.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
