package com.studentfinance;

import com.studentfinance.dao.*;
import com.studentfinance.database.Database;
import com.studentfinance.model.*;
import com.studentfinance.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BudgetAndReportTest {
    @TempDir Path dir;
    final YearMonth month = YearMonth.of(2026, 11);

    private Database open() throws Exception {
        Database database = new Database(dir.resolve("ledger.db"));
        database.initialize();
        return database;
    }

    @Test void comparesBeforeAtAndAboveBudgetBoundaries() {
        Budget budget = new Budget(month, 10000);
        assertEquals("Within budget", budget.status(7999));
        assertEquals("80% warning", budget.status(8000));
        assertEquals("Budget reached", budget.status(10000));
        assertEquals("Budget exceeded", budget.status(10001));
        assertEquals(-1, budget.remaining(10001));
        assertEquals("Within budget", new Budget(month, 101).status(80));
        assertEquals("80% warning", new Budget(month, 101).status(81));
    }

    @Test void rejectsInvalidLimits() {
        for (long value : List.of(-1L, 0L, Transaction.MAX_AMOUNT_PAISE + 1))
            assertThrows(IllegalArgumentException.class, () -> new Budget(month, value));
    }

    @Test void revisesOneMonthAndPersistsIndependentLimits() throws Exception {
        BudgetDao dao = new BudgetDao(open());
        dao.save(new Budget(month, 1000));
        dao.save(new Budget(month.plusMonths(1), 3000));
        dao.save(new Budget(month, 2000));
        Map<YearMonth, Budget> stored = new BudgetDao(open()).findAll();
        assertEquals(2, stored.size());
        assertEquals(2000, stored.get(month).limitPaise());
        assertEquals(3000, stored.get(month.plusMonths(1)).limitPaise());
    }

    @Test void filtersByTypeTextAndMonthTogether() throws Exception {
        FinanceService service = new FinanceService(new SqliteTransactionDao(open()));
        Transaction kept = service.save(new Transaction(0, TransactionType.EXPENSE, 145,
                "Health", month.atDay(1), "Campus clinic"));
        service.save(new Transaction(0, TransactionType.INCOME, 200,
                "Freelance", month.atDay(1), "Campus poster"));
        service.save(new Transaction(0, TransactionType.EXPENSE, 125,
                "Accommodation", month.minusMonths(1).atDay(1), "Campus hostel"));
        var rows = service.filter(service.all(), TransactionType.EXPENSE, " CAMpus ", month);
        assertEquals(List.of(kept.getId()), rows.stream().map(Transaction::getId).toList());
        assertEquals(3, service.filter(service.all(), null, "", null).size());
        assertTrue(service.filter(service.all(), null, "no such record", null).isEmpty());
    }

    @Test void weeksStartMondayAndCountOnlySelectedMonth() throws Exception {
        FinanceService service = new FinanceService(new SqliteTransactionDao(open()));
        for (LocalDate date : List.of(month.atDay(1).minusDays(1), month.atDay(1), month.atDay(2),
                month.atDay(8), month.atDay(9))) {
            service.save(new Transaction(0, TransactionType.EXPENSE, 100, "Food", date, ""));
        }
        var summary = service.summarize(service.all(), month);
        assertEquals(Map.of(LocalDate.of(2026,10,26),100L, month.atDay(2),200L, month.atDay(9),100L),
                summary.weeklyExpenses());
        assertEquals(400, summary.monthlyExpenses());
    }

    @Test void upgradesLegacyLedgerWithoutChangingIdsOrReusingDeletedIds() throws Exception {
        Database db = new Database(dir.resolve("ledger.db"));
        try (Connection c=db.connect(); Statement query=c.createStatement()) {
            query.execute("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,amount_paise INTEGER,category TEXT,transaction_date TEXT,description TEXT)");
            query.execute("INSERT INTO transactions VALUES(8,'INCOME',125,'Gift','2026-11-01','legacy')");
            query.execute("INSERT INTO transactions VALUES(29,'EXPENSE',220,'Food','2026-11-02','deleted')");
            query.execute("DELETE FROM transactions WHERE id=29");
        }
        db.initialize(); db.initialize();
        FinanceService service = new FinanceService(new SqliteTransactionDao(db));
        Transaction original=service.all().get(0);
        assertEquals(8, original.getId());
        assertEquals(125, original.getAmountPaise());
        assertEquals("legacy", original.getDescription());
        Transaction next=service.save(new Transaction(0,TransactionType.EXPENSE,555,"Health",month.atDay(2),""));
        assertTrue(next.getId()>29);
    }

    @Test void failedMigrationRollsBackLegacyTable() throws Exception {
        Database db = new Database(dir.resolve("ledger.db"));
        try (Connection c=db.connect(); Statement s=c.createStatement()) {
            s.execute("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,amount_paise INTEGER,category TEXT,transaction_date TEXT,description TEXT)");
            s.execute("INSERT INTO transactions VALUES(1,'EXPENSE',0,'Food','2026-11-01','invalid legacy row')");
        }
        assertThrows(SQLException.class, db::initialize);
        try (Connection c=db.connect(); Statement s=c.createStatement(); ResultSet rows=s.executeQuery("SELECT description FROM transactions")) {
            assertTrue(rows.next());
            assertEquals("invalid legacy row", rows.getString(1));
        }
    }
}
