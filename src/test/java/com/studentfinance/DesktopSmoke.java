package com.studentfinance;

import com.studentfinance.ui.MainFrame;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.FutureTask;
import java.util.function.*;

/** Drive the packaged application's Swing controls against a fresh demo database. */
public final class DesktopSmoke {
    public static void main(String[] args) throws Exception {
        try {
            App.main(new String[]{"--demo"});
            MainFrame frame = waitFor(() -> {
                for (Window w : Window.getWindows()) {
                    if (w instanceof MainFrame main && main.isShowing() && !main.isLoading()) return main;
                }
                return null;
            });
            onUi(() -> {
                button(frame, "Budgets").doClick();
                JButton save = button(frame, "Save limit");
                textField(save.getParent()).setText("2000");
                save.doClick();
                return true;
            });
            waitFor(() -> !frame.isLoading() && find(frame, JLabel.class,
                    l -> l.isShowing() && l.getText().contains("Budget exceeded")) != null ? true : null);
            onUi(() -> {
                button(frame, "Expenses").doClick();
                JTextField search = find(frame, JTextField.class, f -> f.isShowing() && f.getColumns() == 18);
                search.setText("groceries");
                JTable ledger = find(frame, JTable.class, Component::isShowing);
                if (ledger.getRowCount() != 1 || !ledger.getValueAt(0, 2).equals("Food"))
                    throw new AssertionError("Search did not find the seeded grocery entry.");
                button(frame, "Reports").doClick();
                JTabbedPane reports = find(frame, JTabbedPane.class, Component::isShowing);
                reports.setSelectedIndex(reports.indexOfTab("Weekly"));
                if (find(reports, JTable.class, Component::isShowing).getRowCount() < 2)
                    throw new AssertionError("Weekly report is missing rows.");
                button(frame, "Settings").doClick();
                JCheckBox recent = find(frame, JCheckBox.class, Component::isShowing);
                recent.doClick();
                button(frame, "Dashboard").doClick();
                if (find(frame, JTable.class, Component::isShowing) != null)
                    throw new AssertionError("Dashboard preference did not apply.");
                frame.dispose();
                return true;
            });
            System.out.println("DESKTOP_OK packaged launch, budget edit, warning, search, weekly reports, settings");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static JButton button(Container c, String label) {
        JButton result = find(c, JButton.class, b -> b.isShowing() && label.equals(b.getText()));
        if (result == null) throw new AssertionError("Missing visible button " + label);
        return result;
    }
    private static JTextField textField(Container c) {
        return find(c, JTextField.class, Component::isShowing);
    }
    private static <T> T find(Container container, Class<T> type, Predicate<T> match) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child) && match.test(type.cast(child))) return type.cast(child);
            if (child instanceof Container nested) {
                T result = find(nested, type, match);
                if (result != null) return result;
            }
        }
        return null;
    }
    private static <T> T onUi(Supplier<T> action) throws Exception {
        FutureTask<T> result = new FutureTask<>(action::get);
        SwingUtilities.invokeAndWait(result);
        return result.get();
    }
    private static <T> T waitFor(Supplier<T> action) throws Exception {
        for (int i = 0; i < 100; i++) {
            T result = onUi(action);
            if (result != null) return result;
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for the desktop application");
    }
}
