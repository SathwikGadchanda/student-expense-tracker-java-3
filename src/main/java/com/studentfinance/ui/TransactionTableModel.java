package com.studentfinance.ui;

import com.studentfinance.model.Transaction;
import com.studentfinance.util.Money;
import javax.swing.table.AbstractTableModel;
import java.util.List;

/** Swing calls these overridden methods polymorphically when it paints the table. */
public final class TransactionTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Date", "Type", "Category", "Amount (INR)", "Description"};
    private List<Transaction> transactions = List.of();

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = List.copyOf(transactions);
        fireTableDataChanged();
    }

    public Transaction getTransaction(int row) { return transactions.get(row); }
    @Override public int getRowCount() { return transactions.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }
    @Override public Class<?> getColumnClass(int column) {
        return column == 0 ? java.time.LocalDate.class : column == 3 ? java.math.BigDecimal.class : String.class;
    }
    @Override public Object getValueAt(int row, int column) {
        Transaction transaction = transactions.get(row);
        return switch (column) {
            case 0 -> transaction.getDate();
            case 1 -> transaction.getType().toString();
            case 2 -> transaction.getCategory();
            case 3 -> new java.math.BigDecimal(Money.decimal(transaction.getAmountPaise()));
            case 4 -> transaction.getDescription();
            default -> throw new IndexOutOfBoundsException();
        };
    }
}
