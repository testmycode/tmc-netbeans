package fi.helsinki.cs.tmc.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;

public class ProgressBarDescription extends JPanel {
    private final Color color;
    private final String text;
    private final int x;
    private final int y;
    private final int size;

    public ProgressBarDescription(int x, int y, String text, Color color) {
        this.color = color;
        this.text = text;
        this.x = x;
        this.y = y;
        this.size = 10;
        this.setPreferredSize(new Dimension(100, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawRect(x, y, size, size);
        g.setColor(color);
        g.fillRect(x+1, y+1, size-2, size-2);

        g.setColor(Color.BLACK);
        g.drawString(text, x + size + 5, y + size);
    }

    public Color getColor() {
        return color;
    }

}
