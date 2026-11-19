package com.studentfinance.service;

import com.studentfinance.dao.TransactionDao;
import com.studentfinance.model.Transaction;
import com.studentfinance.model.TransactionType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public final class FinanceService {
    private final TransactionDao dao;

    public FinanceService(TransactionDao dao) { this.dao = Objects.requireNonNull(dao); }
    public List<Transaction> all() throws SQLException { return dao.findAll(); }
    public Transaction save(Transaction transaction) throws SQLException {
        if (transaction.getId() == 0) return dao.insert(transaction);
        dao.update(transaction);
        return transaction;
    }
    public void delete(long id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("Select a transaction first.");
        dao.delete(id);
    }

    public List<Transaction> filter(List<Transaction> records, TransactionType type, String query, YearMonth month) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return records.stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> month == null || YearMonth.from(t.getDate()).equals(month))
                .filter(t -> (t.getCategory() + " " + t.getDescription() + " " + t.getDate()
                        + " " + t.getType()).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public FinanceSummary summarize(List<Transaction> transactions, YearMonth month) {
        long income = 0, expenses = 0, monthIncome = 0, monthExpenses = 0;
        Map<String, Long> categories = new TreeMap<>();
        Map<LocalDate, Long> days = new TreeMap<>();
        Map<YearMonth, Long> history = new TreeMap<>();
        for (Transaction transaction : transactions) {
            boolean selectedMonth = YearMonth.from(transaction.getDate()).equals(month);
            if (transaction.getType() == TransactionType.INCOME) {
                income = Math.addExact(income, transaction.getAmountPaise());
                if (selectedMonth) monthIncome = Math.addExact(monthIncome, transaction.getAmountPaise());
            } else {
                expenses = Math.addExact(expenses, transaction.getAmountPaise());
                history.merge(YearMonth.from(transaction.getDate()), transaction.getAmountPaise(), Math::addExact);
                if (selectedMonth) {
                    monthExpenses = Math.addExact(monthExpenses, transaction.getAmountPaise());
                    categories.merge(transaction.getCategory(), transaction.getAmountPaise(), Math::addExact);
                    days.merge(transaction.getDate(), transaction.getAmountPaise(), Math::addExact);
                }
            }
        }
        return new FinanceSummary(income, expenses, Math.subtractExact(income, expenses), monthIncome,
                monthExpenses, month, Collections.unmodifiableMap(categories),
                Collections.unmodifiableMap(days), Collections.unmodifiableMap(history));
    }
}
