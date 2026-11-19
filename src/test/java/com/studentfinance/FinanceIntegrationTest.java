package com.studentfinance;

import com.studentfinance.dao.*;
import com.studentfinance.database.Database;
import com.studentfinance.model.*;
import com.studentfinance.service.*;
import com.studentfinance.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FinanceIntegrationTest {
    @TempDir Path temp;
    private Database database;
    private FinanceService service;
    private final LocalDate day = LocalDate.of(2026, 11, 18);

    @BeforeEach void initialize() throws Exception {
        database = new Database(temp.resolve("nested/test.db"));
        database.initialize();
        service = new FinanceService(new SqliteTransactionDao(database));
    }

    private Transaction record(TransactionType type, String amount, String category, LocalDate date, String text) throws Exception {
        return service.save(new Transaction(0, type, Money.parsePaise(amount), category, date, text));
    }

    private Transaction expense(String amount, String category, LocalDate date) throws Exception {
        return record(TransactionType.EXPENSE, amount, category, date, "College purchase");
    }

    @Test void createsDatabaseAndConnects() throws Exception {
        assertTrue(Files.exists(database.getPath()));
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT 1")) {
            assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
        }
    }

    @Test void initializationIsIdempotent() throws Exception {
        expense("120", "Food", day); database.initialize(); assertEquals(1, service.all().size());
    }

    @Test void addsIncomeAndExpenseWithUniqueIds() throws Exception {
        Transaction income = record(TransactionType.INCOME, "5000", "Pocket Money", day, "Allowance");
        Transaction expense = expense("125.50", "Food", day);
        assertTrue(income.getId() > 0); assertTrue(expense.getId() > income.getId());
        assertEquals(2, service.all().size());
    }

    @Test void updatesAllTransactionFields() throws Exception {
        Transaction original = expense("120", "Food", day);
        service.save(new Transaction(original.getId(), TransactionType.INCOME, 90000,
                "Gift", day.minusDays(1), "Birthday"));
        Transaction saved = service.all().get(0);
        assertEquals(TransactionType.INCOME, saved.getType()); assertEquals(90000, saved.getAmountPaise());
        assertEquals("Gift", saved.getCategory()); assertEquals(day.minusDays(1), saved.getDate());
        assertEquals("Birthday", saved.getDescription());
    }

    @Test void deletesSelectedRecordOnly() throws Exception {
        Transaction first = expense("120", "Food", day);
        Transaction second = expense("80", "Transportation", day);
        service.delete(first.getId()); assertEquals(second.getId(), service.all().get(0).getId());
        assertEquals(1, service.all().size());
    }

    @Test void missingRecordsRaiseUsefulErrors() {
        assertThrows(SQLException.class, () -> service.delete(200));
        assertThrows(SQLException.class, () -> service.save(new Transaction(200, TransactionType.EXPENSE,
                100, "Other", day, "Missing")));
    }

    @Test void calculatesIncomeExpensesAndBalanceExactly() throws Exception {
        record(TransactionType.INCOME, "1000.10", "Pocket Money", day, "Allowance");
        expense("100.05", "Food", day); expense("50.02", "Other", day);
        FinanceSummary summary = service.summarize(service.all(), YearMonth.from(day));
        assertEquals(100010, summary.totalIncome()); assertEquals(15007, summary.totalExpenses());
        assertEquals(85003, summary.balance());
    }

    @Test void monthAndDayBoundariesDoNotMix() throws Exception {
        expense("100", "Food", LocalDate.of(2026, 10, 31));
        expense("200", "Food", LocalDate.of(2026, 11, 1));
        expense("300", "Education", LocalDate.of(2026, 11, 30));
        expense("400", "Food", LocalDate.of(2026, 12, 1));
        record(TransactionType.INCOME, "8000", "Pocket Money", day, "Allowance");
        FinanceSummary summary = service.summarize(service.all(), YearMonth.of(2026, 11));
        assertEquals(50000, summary.monthlyExpenses()); assertEquals(800000, summary.monthlyIncome());
        assertEquals(20000, summary.dailyExpenses().get(LocalDate.of(2026, 11, 1)));
        assertEquals(30000, summary.categoryExpenses().get("Education"));
        assertEquals(3, summary.monthlyHistory().size());
    }

    @Test void sameDayAndCategoryAreAggregated() throws Exception {
        expense("12.50", "Food", day); expense("17.50", "Food", day);
        FinanceSummary summary = service.summarize(service.all(), YearMonth.from(day));
        assertEquals(3000, summary.dailyExpenses().get(day)); assertEquals(3000, summary.categoryExpenses().get("Food"));
    }

    @Test void handlesYearBoundaryAndLeapDay() throws Exception {
        expense("50", "Food", LocalDate.of(2024, 2, 29));
        expense("70", "Food", LocalDate.of(2025, 12, 31));
        expense("80", "Food", LocalDate.of(2026, 1, 1));
        assertEquals(5000, service.summarize(service.all(), YearMonth.of(2024, 2)).monthlyExpenses());
        assertEquals(8000, service.summarize(service.all(), YearMonth.of(2026, 1)).monthlyExpenses());
    }

    @Test void emptyDatabaseAndMonthReturnZero() throws Exception {
        FinanceSummary summary = service.summarize(service.all(), YearMonth.from(day));
        assertEquals(0, summary.balance()); assertTrue(summary.categoryExpenses().isEmpty());
        expense("10", "Food", day);
        assertEquals(0, service.summarize(service.all(), YearMonth.of(2020, 1)).monthlyExpenses());
    }

    @Test void permitsNegativeBalance() throws Exception {
        expense("100", "Education", day);
        assertEquals(-10000, service.summarize(service.all(), YearMonth.from(day)).balance());
    }

    @Test void newDatabaseAndServiceInstancesReadPersistedData() throws Exception {
        Transaction saved = expense("123.45", "Food", day);
        Database reopened = new Database(database.getPath()); reopened.initialize();
        FinanceService restarted = new FinanceService(new SqliteTransactionDao(reopened));
        assertEquals(saved.getId(), restarted.all().get(0).getId());
        assertEquals(12345, restarted.all().get(0).getAmountPaise());
    }

    @Test void rejectsInvalidAmountsAndCategories() {
        for (String amount : List.of("", "abc", "1.001", "1e4", "-1", "NaN", "1,000", "9999999999999999999999"))
            assertThrows(IllegalArgumentException.class, () -> Money.parsePaise(amount));
        for (long amount : List.of(0L, -1L, Transaction.MAX_AMOUNT_PAISE + 1))
            assertThrows(IllegalArgumentException.class, () -> new Transaction(0, TransactionType.EXPENSE, amount, "Food", day, ""));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(0, TransactionType.INCOME, 100, "Food", day, ""));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(0, TransactionType.EXPENSE, 100, "Food", day, "x".repeat(251)));
    }

    @Test void databaseConstraintsRejectInvalidDirectWrites() throws Exception {
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO transactions(type,amount_paise,category,transaction_date) VALUES ('EXPENSE',0,'Food','2026-11-18')"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO transactions(type,amount_paise,category,transaction_date) VALUES ('INCOME',100,'Food','2026-11-18')"));
        }
    }

    @Test void preparedStatementsPreserveQuotesAndSqlAsData() throws Exception {
        String input = "O'Brien'); DROP TABLE transactions; --";
        record(TransactionType.EXPENSE, "99", "Other", day, input);
        assertEquals(input, service.all().get(0).getDescription());
        expense("10", "Food", day); assertEquals(2, service.all().size());
    }

    @Test void exportsUtf8CsvWithCommasQuotesAndMultilineFields() throws Exception {
        record(TransactionType.EXPENSE, "12.50", "Food", day, "Café, \"meal\"\nSecond line");
        Path csv = temp.resolve("transactions.csv"); CsvExporter.export(csv, service.all());
        String text = Files.readString(csv);
        assertTrue(text.startsWith("ID,Type,Amount (INR),Category,Date,Description\r\n"));
        assertTrue(text.contains("12.50")); assertTrue(text.contains("\"Café, \"\"meal\"\"\nSecond line\""));
    }

    @Test void csvNeutralizesSpreadsheetFormulasAndReplacesFile() throws Exception {
        record(TransactionType.EXPENSE, "10", "Other", day, "=HYPERLINK(\"https://example.com\")");
        Path csv = temp.resolve("safe.csv"); Files.writeString(csv, "old content");
        CsvExporter.export(csv, service.all()); String text = Files.readString(csv);
        assertTrue(text.contains("\"'=HYPERLINK(")); assertFalse(text.contains("old content"));
    }

    @Test void failedCsvWriteDoesNotDamageExistingFile() throws Exception {
        Path existing = temp.resolve("keep.csv"); Files.writeString(existing, "important");
        assertThrows(java.io.IOException.class, () -> CsvExporter.export(temp.resolve("missing/export.csv"), List.of()));
        assertEquals("important", Files.readString(existing));
    }

    @Test void resultsSortByDateThenIdAndAreImmutable() throws Exception {
        expense("1", "Food", day); Transaction latest = expense("2", "Other", day.plusDays(1));
        List<Transaction> records = service.all(); assertEquals(latest.getId(), records.get(0).getId());
        assertThrows(UnsupportedOperationException.class, records::clear);
    }
}
