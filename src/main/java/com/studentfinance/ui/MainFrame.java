package com.studentfinance.ui;

import com.studentfinance.model.*;
import com.studentfinance.dao.BudgetDao;
import com.studentfinance.service.*;
import com.studentfinance.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class MainFrame extends JFrame {
    private final FinanceService service;
    private final BudgetDao budgetDao;
    private Map<YearMonth, Budget> budgets = Map.of();
    private final JTextField budgetLimit = new JTextField(12);
    private final JLabel budgetStatus = Theme.label(" ", 20, Theme.GREEN, true);
    private final JLabel budgetNumbers = Theme.label(" ", 16, Theme.TEXT, false);
    private final JLabel dashboardBudget = Theme.label(" ", 13, Theme.GREEN, true);
    private final JLabel ledgerTotal = Theme.label(" ", 13, Theme.TEXT, true);
    private final JCheckBox onlyMonth = new JCheckBox("Selected month only");
    private final TransactionTableModel recent = new TransactionTableModel();
    private JPanel recentSection;
    private final DefaultTableModel weekly = reportModel("Week beginning Monday", "Expense (INR)");
    private final DefaultTableModel comparison = reportModel("Selected month", "Amount (INR)");
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHost = new JPanel(pages);
    private final JLabel title = Theme.label("Dashboard", 28, Theme.GREEN, true);
    private final JLabel status = Theme.label("Loading records...", 12, Theme.MUTED, false);
    private final JLabel income = Theme.label("INR 0.00", 26, Theme.GREEN, true);
    private final JLabel expenses = Theme.label("INR 0.00", 26, Theme.TEXT, true);
    private final JLabel balance = Theme.label("INR 0.00", 26, Theme.GREEN, true);
    private final JLabel monthly = Theme.label("INR 0.00", 26, Theme.TEXT, true);
    private final JLabel monthDetail = Theme.label(" ", 13, Theme.MUTED, false);
    private final JTextField month = new JTextField(YearMonth.now().toString(), 7);
    private YearMonth selectedMonth = YearMonth.now();
    private final TransactionTableModel transactions = new TransactionTableModel();
    private final JTable table = new JTable(transactions);
    private final CategoryChartPanel dashboardChart = new CategoryChartPanel();
    private final CategoryChartPanel reportChart = new CategoryChartPanel();
    private final DefaultTableModel daily = reportModel("Date", "Expense (INR)");
    private final DefaultTableModel category = reportModel("Category", "Expense (INR)");
    private final DefaultTableModel history = reportModel("Month", "Expense (INR)");
    private final JTextField search = new JTextField(18);
    private List<Transaction> snapshot = List.of();
    private String currentPage = "Dashboard";
    private boolean busy;

    public MainFrame(FinanceService service, BudgetDao budgetDao, Path databasePath, boolean demo) {
        super("Student Expense Tracker" + (demo ? " - Demo data" : ""));
        this.service = service;
        this.budgetDao = budgetDao;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 900);
        setMinimumSize(new Dimension(1120, 740));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(sidebar(demo), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(Theme.BACKGROUND);
        main.setBorder(new EmptyBorder(28, 28, 16, 28));
        main.add(header(), BorderLayout.NORTH);
        pageHost.setOpaque(false);
        pageHost.add(dashboard(), "Dashboard");
        pageHost.add(transactionPage(), "Transactions");
        pageHost.add(reports(), "Reports");
        pageHost.add(budgetPage(), "Budgets");
        pageHost.add(settings(databasePath, demo), "Settings");
        main.add(pageHost, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(status, BorderLayout.WEST);
        JLabel local = Theme.label(demo ? "DEMO DATA  /  Separate temporary database" : "Saved on this device", 11, Theme.MUTED, false);
        local.setToolTipText(databasePath.toString());
        footer.add(local, BorderLayout.EAST);
        main.add(footer, BorderLayout.SOUTH);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
        refresh(null);
    }

    private JPanel sidebar(boolean demo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.GREEN);
        panel.setBorder(new EmptyBorder(34, 22, 28, 22));
        panel.setPreferredSize(new Dimension(214, 0));
        JLabel brand = Theme.label("STUDENT", 23, Color.WHITE, true);
        JLabel sub = Theme.label("Expense Tracker", 16, new Color(202, 229, 211), false);
        panel.add(brand);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sub);
        panel.add(Box.createVerticalStrut(44));
        for (String name : List.of("Dashboard", "Transactions", "Income", "Expenses", "Budgets", "Reports", "Settings")) {
            JButton button = Theme.button(name, true);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(175, 48));
            button.setBorder(new EmptyBorder(13, 10, 13, 8));
            button.addActionListener(event -> showPage(name));
            panel.add(button);
            panel.add(Box.createVerticalStrut(10));
        }
        panel.add(Box.createVerticalGlue());
        if (demo) panel.add(Theme.label("DEMONSTRATION", 10, new Color(202, 229, 211), true));
        panel.add(Box.createVerticalStrut(10));
        panel.add(Theme.label("Java 17  /  SQLite", 11, new Color(202, 229, 211), false));
        return panel;
    }

    private JPanel header() {
        JPanel outer = new JPanel(new BorderLayout(0, 18));
        outer.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel names = new JPanel(new GridLayout(2, 1, 0, 5));
        names.setOpaque(false);
        names.add(title);
        names.add(dashboardBudget);
        top.add(names, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        JButton addIncome = Theme.button("+ Income", false);
        JButton addExpense = Theme.button("+ Expense", true);
        addIncome.addActionListener(event -> edit(null, TransactionType.INCOME));
        addExpense.addActionListener(event -> edit(null, TransactionType.EXPENSE));
        actions.add(addIncome);
        actions.add(addExpense);
        top.add(actions, BorderLayout.EAST);
        outer.add(top, BorderLayout.NORTH);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filter.setOpaque(false);
        JLabel label = Theme.label("Report month", 13, Theme.TEXT, true);
        label.setLabelFor(month);
        filter.add(label);
        month.setToolTipText("Enter a month as YYYY-MM, then Apply");
        filter.add(month);
        JButton apply = Theme.button("Apply", false);
        apply.addActionListener(event -> applyMonth());
        month.addActionListener(event -> applyMonth());
        filter.add(apply);
        JButton reload = Theme.button("Refresh", false);
        reload.addActionListener(event -> { if (!busy) refresh(null); });
        filter.add(reload);
        outer.add(filter, BorderLayout.SOUTH);
        return outer;
    }

    private void applyMonth() {
        try {
            if (!month.getText().strip().matches("\\d{4}-\\d{2}")) throw new IllegalArgumentException();
            selectedMonth = YearMonth.parse(month.getText().strip());
            render();
        } catch (Exception invalid) { error("Enter a valid month as YYYY-MM, for example 2026-11."); }
    }

    private JPanel dashboard() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(card("TOTAL INCOME", income, "All recorded income"));
        cards.add(card("TOTAL EXPENSES", expenses, "All recorded expenses"));
        cards.add(card("REMAINING BALANCE", balance, "Income minus expenses"));
        cards.add(card("MONTHLY SPENDING", monthly, "Selected report month"));
        panel.add(cards, BorderLayout.NORTH);
        JPanel chart = section("Spending by category", dashboardChart);
        chart.add(monthDetail, BorderLayout.SOUTH);
        JTable recentTable = new JTable(recent);
        Theme.configureTable(recentTable);
        recentTable.setRowHeight(36);
        recentTable.removeColumn(recentTable.getColumnModel().getColumn(4));
        recentTable.removeColumn(recentTable.getColumnModel().getColumn(1));
        recentSection = section("Recent transactions", new JScrollPane(recentTable));
        JPanel details = new JPanel(new BorderLayout(14, 0));
        details.setOpaque(false);
        recentSection.setPreferredSize(new Dimension(330, 300));
        details.add(chart, BorderLayout.CENTER);
        details.add(recentSection, BorderLayout.EAST);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private JPanel card(String caption, JLabel value, String hint) {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE), new EmptyBorder(18, 17, 18, 10)));
        card.add(Theme.label(caption, 10, Theme.MUTED, true), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(Theme.label(hint, 11, Theme.MUTED, false), BorderLayout.SOUTH);
        return card;
    }

    private JPanel transactionPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        Theme.configureTable(table);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel find = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        find.setOpaque(false);
        JLabel label = Theme.label("Search", 13, Theme.TEXT, true);
        label.setLabelFor(search);
        find.add(label);
        find.add(search);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { filterTransactions(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { filterTransactions(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { filterTransactions(); }
        });
        onlyMonth.setOpaque(false);
        onlyMonth.addActionListener(event -> filterTransactions());
        find.add(onlyMonth);
        toolbar.add(find, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton edit = Theme.button("Edit selected", false);
        edit.addActionListener(event -> { Transaction transaction = selected(); if (transaction != null) edit(transaction, transaction.getType()); });
        JButton delete = Theme.button("Delete selected", false);
        delete.addActionListener(event -> deleteSelected());
        JButton export = Theme.button("Export all to CSV", true);
        export.addActionListener(event -> export());
        actions.add(edit); actions.add(delete); actions.add(export);
        toolbar.add(actions, BorderLayout.SOUTH);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(ledgerTotal, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel reports() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        reportChart.setPreferredSize(new Dimension(500, 246));
        panel.add(section("Category totals for the selected month", reportChart), BorderLayout.NORTH);
        JTabbedPane tables = new JTabbedPane();
        tables.addTab("Income vs expenses", scrollTable(comparison));
        tables.addTab("Daily", scrollTable(daily));
        tables.addTab("Weekly", scrollTable(weekly));
        tables.addTab("By category", scrollTable(category));
        tables.addTab("Monthly history", scrollTable(history));
        panel.add(tables, BorderLayout.CENTER);
        return panel;
    }

    private JPanel budgetPage() {
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setBorder(new EmptyBorder(22, 16, 22, 16));
        budgetStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        budgetNumbers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        fields.add(budgetStatus);
        fields.add(Box.createVerticalStrut(12));
        fields.add(budgetNumbers);
        fields.add(Box.createVerticalStrut(24));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(new JLabel("Monthly limit (INR)"));
        form.add(budgetLimit);
        JButton save = Theme.button("Save limit", true);
        save.addActionListener(event -> saveBudget());
        budgetLimit.addActionListener(event -> saveBudget());
        form.add(save);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        fields.add(form);
        fields.add(Box.createVerticalStrut(18));
        fields.add(new JLabel("A warning appears at 80% of the limit. The report month selects the budget."));
        return section("Monthly spending limit", fields);
    }

    private void saveBudget() {
        if (busy) return;
        try {
            Budget limit = new Budget(selectedMonth, Money.parsePaise(budgetLimit.getText()));
            background(() -> { budgetDao.save(limit); return budgetDao.findAll(); }, updated -> {
                budgets = updated;
                render();
                status.setText("Budget saved for " + limit.month());
            }, failure -> error(failure.getMessage()));
        } catch (IllegalArgumentException invalid) { error(invalid.getMessage()); }
    }

    private JPanel settings(Path path, boolean demo) {
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(new JLabel(demo ? "Data storage: temporary demo database" : "Database: " + path));
        controls.add(Box.createVerticalStrut(12));
        controls.add(new JLabel("Currency: Indian rupees (INR)"));
        controls.add(Box.createVerticalStrut(18));
        java.util.prefs.Preferences prefs = demo ? null : java.util.prefs.Preferences.userNodeForPackage(MainFrame.class);
        JCheckBox showRecent = new JCheckBox("Show recent transactions on the dashboard",
                prefs == null || prefs.getBoolean("showRecent", true));
        recentSection.setVisible(showRecent.isSelected());
        showRecent.addActionListener(event -> {
            recentSection.setVisible(showRecent.isSelected());
            if (prefs != null) prefs.putBoolean("showRecent", showRecent.isSelected());
        });
        controls.add(showRecent);
        return section("Display and storage", controls);
    }

    private JScrollPane scrollTable(DefaultTableModel model) {
        JTable report = new JTable(model);
        Theme.configureTable(report);
        return new JScrollPane(report);
    }

    private static DefaultTableModel reportModel(String first, String second) {
        return new DefaultTableModel(new Object[]{first, second}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private JPanel section(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.add(Theme.label(title, 16, Theme.GREEN, true), BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public void showPage(String name) {
        currentPage = name;
        title.setText(name);
        pages.show(pageHost, name.equals("Expenses") || name.equals("Income") ? "Transactions" : name);
        filterTransactions();
    }

    private void filterTransactions() {
        TransactionType type = switch (currentPage) {
            case "Expenses" -> TransactionType.EXPENSE;
            case "Income" -> TransactionType.INCOME;
            default -> null;
        };
        List<Transaction> records = service.filter(snapshot, type, search.getText(), onlyMonth.isSelected() ? selectedMonth : null);
        transactions.setTransactions(records);
        String totals = type == null ? "" : "    Total: " + Money.format(records.stream().mapToLong(Transaction::getAmountPaise).sum());
        ledgerTotal.setText(records.size() + " matching transactions" + totals);
    }

    private Transaction selected() {
        if (busy) return null;
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a transaction in the table first."); return null; }
        return transactions.getTransaction(table.convertRowIndexToModel(row));
    }

    private void edit(Transaction existing, TransactionType initial) {
        if (busy) return;
        final TransactionDialog[] dialog = new TransactionDialog[1];
        dialog[0] = new TransactionDialog(this, initial, existing, transaction ->
                background(() -> { service.save(transaction); return service.all(); }, records -> {
                    snapshot = records; render(); dialog[0].dispose(); status.setText("Transaction saved.");
                }, failure -> dialog[0].showError(failure.getMessage())));
        dialog[0].setVisible(true);
    }

    private void deleteSelected() {
        Transaction transaction = selected();
        if (transaction == null) return;
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Delete " + transaction.getType().toString().toLowerCase() + " of " + Money.format(transaction.getAmountPaise())
                        + " dated " + transaction.getDate() + "? This cannot be undone.",
                "Delete transaction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation == JOptionPane.YES_OPTION) background(() -> {
            service.delete(transaction.getId()); return service.all();
        }, records -> { snapshot = records; render(); status.setText("Transaction deleted."); }, failure -> error(failure.getMessage()));
    }

    private void export() {
        if (busy) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export all income and expenses");
        chooser.setSelectedFile(new java.io.File("student-transactions.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destination = chooser.getSelectedFile().toPath();
        if (!destination.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
            destination = destination.resolveSibling(destination.getFileName() + ".csv");
        if (java.nio.file.Files.exists(destination) && JOptionPane.showConfirmDialog(this,
                "Replace the existing file?", "Export CSV", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Path target = destination;
        List<Transaction> data = snapshot;
        background(() -> { CsvExporter.export(target, data); return target; }, saved -> {
            status.setText("Exported " + data.size() + " transactions.");
            JOptionPane.showMessageDialog(this, "CSV saved to:\n" + saved.toAbsolutePath());
        }, failure -> error("Could not export CSV: " + failure.getMessage()));
    }

    public void refresh(Runnable after) {
        record Data(List<Transaction> records, Map<YearMonth, Budget> limits) { }
        background(() -> new Data(service.all(), budgetDao.findAll()), data -> {
                    snapshot = data.records(); budgets = data.limits(); render(); if (after != null) after.run();
                },
                failure -> error("Could not read the database: " + failure.getMessage()));
    }

    private void render() {
        FinanceSummary summary = service.summarize(snapshot, selectedMonth);
        income.setText(Money.format(summary.totalIncome()));
        expenses.setText(Money.format(summary.totalExpenses()));
        balance.setText(Money.format(summary.balance()));
        balance.setForeground(summary.balance() < 0 ? new Color(164, 58, 46) : Theme.GREEN);
        monthly.setText(Money.format(summary.monthlyExpenses()));
        monthDetail.setText(selectedMonth + "   Income: " + Money.format(summary.monthlyIncome())
                + "   Net change: " + Money.format(summary.monthlyIncome() - summary.monthlyExpenses()));
        dashboardChart.setValues(summary.categoryExpenses());
        reportChart.setValues(summary.categoryExpenses());
        fill(daily, summary.dailyExpenses());
        fill(category, summary.categoryExpenses());
        fill(history, summary.monthlyHistory());
        fill(weekly, summary.weeklyExpenses());
        comparison.setRowCount(0);
        comparison.addRow(new Object[]{"Income", Money.decimal(summary.monthlyIncome())});
        comparison.addRow(new Object[]{"Expenses", Money.decimal(summary.monthlyExpenses())});
        comparison.addRow(new Object[]{"Net change", Money.decimal(summary.monthlyIncome() - summary.monthlyExpenses())});
        recent.setTransactions(snapshot.stream().limit(4).toList());
        Budget limit = budgets.get(selectedMonth);
        long spent = summary.monthlyExpenses();
        String state = limit == null ? "No budget set" : limit.status(spent);
        budgetStatus.setText(selectedMonth + "   " + state);
        dashboardBudget.setText(selectedMonth + "   /   " + state);
        Color alertColor = limit != null && spent >= limit.limitPaise() ? new Color(155, 53, 46) : Theme.GREEN;
        budgetStatus.setForeground(alertColor);
        dashboardBudget.setForeground(alertColor);
        budgetNumbers.setText("Spent: " + Money.format(spent) + (limit == null ? "" : "     Limit: "
                + Money.format(limit.limitPaise()) + "     Remaining: " + Money.format(limit.remaining(spent))));
        budgetLimit.setText(limit == null ? "" : Money.decimal(limit.limitPaise()));
        filterTransactions();
        status.setText(snapshot.isEmpty() ? "No transactions yet. Start with + Income or + Expense." : snapshot.size() + " transactions saved locally.");
    }

    private void fill(DefaultTableModel model, Map<?, Long> values) {
        model.setRowCount(0);
        values.forEach((key, value) -> model.addRow(new Object[]{key.toString(), Money.decimal(value)}));
    }

    private <T> void background(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        if (busy) return;
        busy = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        status.setText("Working...");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false;
                setCursor(Cursor.getDefaultCursor());
                try { success.accept(get()); }
                catch (Exception error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    status.setText("Operation failed. Your existing records remain available.");
                    failure.accept(cause instanceof Exception exception ? exception : error);
                }
            }
        }.execute();
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Student Expense Tracker", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isLoading() { return busy; }
}
