package com.studentfinance.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {
    public static final Color GREEN = new Color(22, 94, 67);
    public static final Color MINT = new Color(229, 244, 234);
    public static final Color BACKGROUND = new Color(246, 249, 247);
    public static final Color TEXT = new Color(27, 46, 36);
    public static final Color MUTED = new Color(100, 119, 109);
    public static final Color LINE = new Color(220, 231, 223);
    private Theme() { }

    public static void install() {
        Font font = new Font("Segoe UI", Font.PLAIN, 14);
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource)
                UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
        }
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("Table.selectionBackground", MINT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("TextField.selectionBackground", MINT);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("OptionPane.background", Color.WHITE);
    }

    public static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(primary ? GREEN : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : GREEN);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? GREEN : LINE), new EmptyBorder(10, 16, 10, 16)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void configureTable(JTable table) {
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(MINT);
        table.getTableHeader().setForeground(GREEN);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        javax.swing.table.DefaultTableCellRenderer textRenderer = new javax.swing.table.DefaultTableCellRenderer();
        textRenderer.setBorder(new EmptyBorder(0, 8, 0, 8));
        textRenderer.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, textRenderer);
        javax.swing.table.DefaultTableCellRenderer numberRenderer = new javax.swing.table.DefaultTableCellRenderer();
        numberRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        numberRenderer.setBorder(new EmptyBorder(0, 8, 0, 8));
        numberRenderer.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(Number.class, numberRenderer);
    }
}
