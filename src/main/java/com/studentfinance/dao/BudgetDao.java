package com.studentfinance.dao;

import com.studentfinance.database.Database;
import com.studentfinance.model.Budget;
import java.sql.*;
import java.time.YearMonth;
import java.util.*;

public final class BudgetDao {
    private final Database database;

    public BudgetDao(Database database) { this.database = database; }

    public Map<YearMonth, Budget> findAll() throws SQLException {
        Map<YearMonth, Budget> budgets = new TreeMap<>();
        try (Connection c = database.connect();
             PreparedStatement query = c.prepareStatement("SELECT month, limit_paise FROM budgets");
             ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                YearMonth month = YearMonth.parse(rows.getString(1));
                budgets.put(month, new Budget(month, rows.getLong(2)));
            }
        }
        return Map.copyOf(budgets);
    }

    public void save(Budget budget) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement write = c.prepareStatement("""
                INSERT INTO budgets(month,limit_paise) VALUES (?,?)
                ON CONFLICT(month) DO UPDATE SET limit_paise=excluded.limit_paise""")) {
            write.setString(1, budget.month().toString());
            write.setLong(2, budget.limitPaise());
            write.executeUpdate();
        }
    }
}
