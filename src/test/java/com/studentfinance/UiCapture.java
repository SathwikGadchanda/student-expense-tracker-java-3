package com.studentfinance;

import com.studentfinance.dao.SqliteTransactionDao;
import com.studentfinance.database.Database;
import com.studentfinance.service.FinanceService;
import com.studentfinance.ui.*;
import com.studentfinance.util.DemoData;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Captures the actual rendered Swing window using an isolated synthetic demo database. */
public final class UiCapture {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]); Files.createDirectories(output);
        Database database = new Database(Files.createTempDirectory("tracker-screenshots-").resolve("demo.db"));
        database.initialize();
        FinanceService service = new FinanceService(new SqliteTransactionDao(database));
        var budgets = new com.studentfinance.dao.BudgetDao(database);
        DemoData.seed(service, budgets);
        MainFrame[] frame = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            Theme.install(); frame[0] = new MainFrame(service, budgets, database.getPath(), true);
            frame[0].setSize(1280, 960); frame[0].setVisible(true);
        });
        boolean[] loading = {true};
        for (int attempt = 0; attempt < 100 && loading[0]; attempt++) {
            SwingUtilities.invokeAndWait(() -> loading[0] = frame[0].isLoading());
            if (loading[0]) Thread.sleep(100);
        }
        if (loading[0]) throw new IllegalStateException("Database load did not complete.");
        for (String page : new String[]{"Dashboard", "Transactions", "Expenses", "Income", "Budgets", "Reports"}) {
            SwingUtilities.invokeAndWait(() -> {
                frame[0].showPage(page); frame[0].validate();
                BufferedImage image = new BufferedImage(frame[0].getWidth(), frame[0].getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics(); frame[0].paintAll(graphics); graphics.dispose();
                try { ImageIO.write(image, "png", output.resolve(page.toLowerCase() + ".png").toFile()); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
        }
        SwingUtilities.invokeAndWait(() -> frame[0].dispose());
        System.out.println("Captured six Swing application views with synthetic records.");
        System.exit(0);
    }
}
