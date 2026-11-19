package com.studentfinance.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.studentfinance.model.Category;
import com.studentfinance.model.TransactionType;

/** One local SQLite file. No database server, username, password or token. */
public final class Database {
    private final Path path;

    public Database(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public static Path defaultPath() {
        String override = System.getProperty("expense.tracker.db");
        return override == null ? Path.of(System.getProperty("user.home"),
                ".student-expense-tracker", "expenses.db") : Path.of(override);
    }

    public Path getPath() { return path; }

    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    public void initialize() throws SQLException, IOException {
        Files.createDirectories(path.getParent());
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                int version;
                try (ResultSet rows = statement.executeQuery("PRAGMA user_version")) {
                    rows.next();
                    version = rows.getInt(1);
                }
                if (version > 1) throw new SQLException("This database requires a newer version of the application.");
                if (version == 0) installSchema(connection, statement);
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    /** Upgrade the original single-table database and retain existing IDs and amounts. */
    private static void installSchema(Connection connection, Statement statement) throws SQLException {
        boolean legacy;
        try (ResultSet rows = statement.executeQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='transactions'")) {
            legacy = rows.next();
        }
        if (legacy) statement.execute("ALTER TABLE transactions RENAME TO previous_transactions");
        statement.execute("""
                CREATE TABLE categories (
                    type TEXT NOT NULL CHECK(type IN ('INCOME','EXPENSE')),
                    name TEXT NOT NULL,
                    PRIMARY KEY(type,name)
                )""");
        try (PreparedStatement add = connection.prepareStatement("INSERT INTO categories(type,name) VALUES (?,?)")) {
            for (TransactionType type : TransactionType.values()) {
                for (String name : Category.forType(type)) {
                    add.setString(1, type.name());
                    add.setString(2, name);
                    add.addBatch();
                }
            }
            add.executeBatch();
        }
        statement.execute("""
                    CREATE TABLE transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        type TEXT NOT NULL CHECK(type IN ('INCOME', 'EXPENSE')),
                        amount_paise INTEGER NOT NULL CHECK(typeof(amount_paise) = 'integer'
                            AND amount_paise BETWEEN 1 AND 100000000),
                        category TEXT NOT NULL,
                        transaction_date TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '' CHECK(length(description) <= 250),
                        FOREIGN KEY(type,category) REFERENCES categories(type,name)
                    )
                    """);
        if (legacy) {
            String fields = "id,type,amount_paise,category,transaction_date,description";
            statement.execute("INSERT INTO transactions(" + fields + ") SELECT " + fields + " FROM previous_transactions");
            statement.execute("UPDATE sqlite_sequence SET seq=max(seq,COALESCE((SELECT seq FROM sqlite_sequence WHERE name='previous_transactions'),0)) WHERE name='transactions'");
            statement.execute("DROP TABLE previous_transactions");
        }
        statement.execute("CREATE INDEX idx_transactions_date ON transactions(transaction_date)");
        statement.execute("""
                CREATE TABLE budgets (
                    month TEXT NOT NULL PRIMARY KEY CHECK(length(month)=7 AND month GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]' AND substr(month,6,2) BETWEEN '01' AND '12'),
                    limit_paise INTEGER NOT NULL CHECK(typeof(limit_paise)='integer' AND limit_paise BETWEEN 1 AND 100000000)
                )""");
        statement.execute("PRAGMA user_version = 1");
    }
}
