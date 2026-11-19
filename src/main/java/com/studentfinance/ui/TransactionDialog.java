package com.studentfinance.ui;

import com.studentfinance.model.*;
import com.studentfinance.util.Money;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

public final class TransactionDialog extends JDialog {
    private final JComboBox<TransactionType> type = new JComboBox<>(TransactionType.values());
    private final JComboBox<String> category = new JComboBox<>();
    private final JTextField amount = new JTextField();
    private final JTextField date = new JTextField(LocalDate.now().toString());
    private final JTextField description = new JTextField();
    private final JButton save = Theme.button("Save transaction", true);
    private final JLabel error = Theme.label(" ", 12, new Color(160, 49, 49), false);
    private final long id;

    public TransactionDialog(JFrame parent, TransactionType initial, Transaction existing,
                             Consumer<Transaction> onSave) {
        super(parent, existing == null ? "New transaction" : "Edit transaction", true);
        id = existing == null ? 0 : existing.getId();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(new EmptyBorder(24, 28, 24, 28));
        content.add(Theme.label(existing == null ? "Record your transaction" : "Update transaction", 23, Theme.GREEN, true), BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 7));
        addField(fields, "Type", type);
        addField(fields, "Amount in INR (e.g. 150.50)", amount);
        addField(fields, "Category", category);
        addField(fields, "Date (YYYY-MM-DD)", date);
        addField(fields, "Description (optional, up to 250 characters)", description);
        type.addActionListener(event -> updateCategories());
        type.setSelectedItem(existing == null ? initial : existing.getType());
        updateCategories();
        if (existing != null) {
            category.setSelectedItem(existing.getCategory());
            amount.setText(Money.decimal(existing.getAmountPaise()));
            date.setText(existing.getDate().toString());
            description.setText(existing.getDescription());
        }
        content.add(fields, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.add(error, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancel = Theme.button("Cancel", false);
        cancel.addActionListener(event -> dispose());
        actions.add(cancel);
        actions.add(save);
        footer.add(actions, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        save.addActionListener(event -> {
            try {
                if (!date.getText().strip().matches("\\d{4}-\\d{2}-\\d{2}"))
                    throw new IllegalArgumentException("Use date format YYYY-MM-DD.");
                Transaction transaction = new Transaction(id, (TransactionType) type.getSelectedItem(),
                        Money.parsePaise(amount.getText()), (String) category.getSelectedItem(),
                        LocalDate.parse(date.getText().strip()), description.getText());
                save.setEnabled(false);
                onSave.accept(transaction);
            } catch (DateTimeParseException invalidDate) {
                showError("Enter a valid calendar date, such as 2026-11-18.");
            } catch (IllegalArgumentException invalid) {
                showError(invalid.getMessage());
            }
        });
        setContentPane(content);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(570, 650);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void addField(JPanel panel, String name, JComponent field) {
        JLabel label = Theme.label(name, 13, Theme.TEXT, true);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private void updateCategories() {
        category.removeAllItems();
        Category.forType((TransactionType) type.getSelectedItem()).forEach(category::addItem);
    }

    public void showError(String message) {
        error.setText("<html>" + message.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        save.setEnabled(true);
    }
}
