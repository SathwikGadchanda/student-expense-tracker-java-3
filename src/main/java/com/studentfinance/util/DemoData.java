package com.studentfinance.util;

import com.studentfinance.model.*;
import com.studentfinance.service.FinanceService;
import com.studentfinance.dao.BudgetDao;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Synthetic records used only by the explicitly selected --demo mode. */
public final class DemoData {
    private DemoData() { }
    public static void seed(FinanceService service, BudgetDao budgets) throws SQLException {
        LocalDate start = YearMonth.now().atDay(1);
        budgets.save(new Budget(YearMonth.from(start), Money.parsePaise("3500")));
        add(service, TransactionType.INCOME, "8000", "Pocket Money", start, "Monthly allowance");
        add(service, TransactionType.INCOME, "1500", "Part-time Work", start.plusDays(2), "Weekend tutoring");
        add(service, TransactionType.EXPENSE, "120", "Food", start.plusDays(2), "Lunch at college canteen");
        add(service, TransactionType.EXPENSE, "450", "Transportation", start.plusDays(3), "Bus pass");
        add(service, TransactionType.EXPENSE, "850", "Education", start.plusDays(4), "Books and lab supplies");
        add(service, TransactionType.EXPENSE, "380", "Food", start.plusDays(5), "Weekly groceries");
        add(service, TransactionType.EXPENSE, "600", "Shopping", start.plusDays(6), "Backpack");
        add(service, TransactionType.EXPENSE, "250", "Entertainment", start.plusDays(7), "Movie with friends");
        add(service, TransactionType.EXPENSE, "100", "Other", start.plusDays(8), "Laundry");
        add(service, TransactionType.EXPENSE, "200", "Food", start.plusDays(9), "Dinner");
        add(service, TransactionType.INCOME, "5000", "Pocket Money", start.minusMonths(1), "Previous month allowance");
        add(service, TransactionType.EXPENSE, "2100", "Food", start.minusMonths(1).plusDays(4), "Previous month meals");
    }
    private static void add(FinanceService service, TransactionType type, String amount, String category,
                            LocalDate date, String description) throws SQLException {
        service.save(new Transaction(0, type, Money.parsePaise(amount), category, date, description));
    }
}
