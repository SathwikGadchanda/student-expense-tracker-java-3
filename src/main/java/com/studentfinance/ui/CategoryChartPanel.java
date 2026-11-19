package com.studentfinance.ui;

import com.studentfinance.util.Money;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/** Lightweight Swing chart. The reports also expose every value in a table. */
public final class CategoryChartPanel extends JPanel {
    private Map<String, Long> values = Map.of();

    public CategoryChartPanel() {
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setPreferredSize(new Dimension(500, 285));
        getAccessibleContext().setAccessibleName("Expense totals by category for the selected month");
    }

    public void setValues(Map<String, Long> values) { this.values = values; repaint(); }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (values.isEmpty()) {
            g.setColor(Theme.MUTED);
            g.drawString("No expenses in this month. Add an expense to see the summary.", 16, 48);
        } else {
            long max = values.values().stream().mapToLong(Long::longValue).max().orElse(1);
            int y = 12;
            int rowHeight = Math.max(18, Math.min(37, (getHeight() - 16) / values.size()));
            int space = Math.max(10, getWidth() - 300);
            for (Map.Entry<String, Long> entry : values.entrySet()) {
                g.setColor(Theme.TEXT);
                g.drawString(entry.getKey(), 12, y + 14);
                g.setColor(Theme.MINT);
                int barHeight = Math.min(20, rowHeight - 2);
                g.fillRoundRect(132, y, space, barHeight, 6, 6);
                g.setColor(Theme.GREEN);
                g.fillRoundRect(132, y, Math.max(2, (int) ((double) entry.getValue() / max * space)), barHeight, 6, 6);
                g.drawString(Money.format(entry.getValue()), 144 + space, y + 14);
                y += rowHeight;
            }
        }
        g.dispose();
    }
}
