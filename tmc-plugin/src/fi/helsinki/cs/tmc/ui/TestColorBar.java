package fi.helsinki.cs.tmc.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JProgressBar;

class TestColorBar extends JProgressBar {

    private static final Color PASS_COLOR = new Color(0x00C800);
    private static final Color FAIL_COLOR = new Color(0xE10000);
    private static final Color VALIDATION_COLOR = new Color(0xFFD000);
    private static final Color UNSET_COLOR = new Color(0xEEEEEE);
    private boolean validationFail;

    public TestColorBar() {
        validationFail = false;
        setStringPainted(true);
    }

    public void validationFail(){
        validationFail = true;
    }

    @Override
    public String getString() {
        if (getMaximum() - getMinimum() > 0) {
            return super.getString();
        } else {
            return "(no tests)";
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        Color oldColor = g.getColor();

        try {
            int w = getWidth();
            int h = getHeight();
            g.clearRect(0, 0, w, h);

            if (!isIndeterminate()) {
                int range = (getMaximum() - getMinimum());
                int filled;
                if (range > 0) {
                    filled = w * getValue() / range;
                } else {
                    filled = w;
                }
                int notFilled = w - filled;
                if(validationFail){
                    g.setColor(VALIDATION_COLOR);
                }else{
                    g.setColor(PASS_COLOR);
                }
                g.fillRect(0, 0, filled, h);
                g.setColor(FAIL_COLOR);
                g.fillRect(filled, 0, notFilled, h);

                if (isStringPainted()) {
                    g.setColor(Color.BLACK);
                    String s = getString();
                    FontMetrics fm = g.getFontMetrics();
                    Rectangle textBox = fm.getStringBounds(s, g).getBounds();

                    int midX = w / 2;
                    int midY = h / 2;
                    int textX = midX - textBox.width / 2;
                    int textY = midY + textBox.height / 2 - fm.getDescent();

                    g.drawString(s, textX, textY);
                }
            } else {
                g.setColor(UNSET_COLOR);
                g.fillRect(0, 0, w, h);
            }

        } finally {
            g.setColor(oldColor);
        }
    }


}
