package com.studentfinance.dao;

import com.studentfinance.database.Database;
import com.studentfinance.model.Transaction;
import com.studentfinance.model.TransactionType;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SqliteTransactionDao implements TransactionDao {
    private final Database database;

    public SqliteTransactionDao(Database database) { this.database = database; }

    @Override
    public List<Transaction> findAll() throws SQLException {
        List<Transaction> result = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM transactions ORDER BY transaction_date DESC, id DESC");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new Transaction(rows.getLong("id"),
                        TransactionType.valueOf(rows.getString("type")), rows.getLong("amount_paise"),
                        rows.getString("category"), LocalDate.parse(rows.getString("transaction_date")),
                        rows.getString("description")));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public Transaction insert(Transaction transaction) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO transactions (type, amount_paise, category, transaction_date, description)
                     VALUES (?, ?, ?, ?, ?)
                     """)) {
            bind(statement, transaction);
            statement.executeUpdate();
            try (Statement idQuery = connection.createStatement();
                 ResultSet row = idQuery.executeQuery("SELECT last_insert_rowid()")) {
                if (!row.next()) throw new SQLException("No ID returned after insert.");
                return transaction.withId(row.getLong(1));
            }
        }
    }

    @Override
    public void update(Transaction transaction) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE transactions SET type=?, amount_paise=?, category=?, transaction_date=?, description=?
                     WHERE id=?
                     """)) {
            bind(statement, transaction);
            statement.setLong(6, transaction.getId());
            if (statement.executeUpdate() != 1) throw new SQLException("Transaction no longer exists. Refresh and try again.");
        }
    }

    @Override
    public void delete(long id) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transactions WHERE id=?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) throw new SQLException("Transaction no longer exists. Refresh and try again.");
        }
    }

    private static void bind(PreparedStatement statement, Transaction transaction) throws SQLException {
        statement.setString(1, transaction.getType().name());
        statement.setLong(2, transaction.getAmountPaise());
        statement.setString(3, transaction.getCategory());
        statement.setString(4, transaction.getDate().toString());
        statement.setString(5, transaction.getDescription());
    }
}
