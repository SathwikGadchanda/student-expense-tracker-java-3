package com.studentfinance;

import com.studentfinance.dao.SqliteTransactionDao;
import com.studentfinance.dao.BudgetDao;
import com.studentfinance.database.Database;
import com.studentfinance.service.FinanceService;
import com.studentfinance.ui.*;
import com.studentfinance.util.DemoData;
import javax.swing.*;
import java.nio.file.*;
import java.util.Arrays;

public final class App {
    private App() { }

    public static void main(String[] args) {
        boolean demo = Arrays.asList(args).contains("--demo");
        try {
            Path databasePath = demo ? Files.createTempDirectory("expense-tracker-demo-").resolve("demo.db") : Database.defaultPath();
            Database database = new Database(databasePath);
            database.initialize();
            FinanceService service = new FinanceService(new SqliteTransactionDao(database));
            BudgetDao budgets = new BudgetDao(database);
            if (demo) DemoData.seed(service, budgets);
            if (Arrays.asList(args).contains("--check-db")) {
                System.out.println("DATABASE_OK records=" + service.all().size());
                return;
            }
            SwingUtilities.invokeLater(() -> {
                Theme.install();
                new MainFrame(service, budgets, database.getPath(), demo).setVisible(true);
            });
        } catch (Exception error) {
            System.err.println("Unable to start Student Expense Tracker: " + error.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Could not open the database. Check that the data folder is writable.\n" + error.getMessage(),
                    "Startup error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
