package com.studentfinance.util;

import com.studentfinance.model.Transaction;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() { }

    public static void export(Path destination, List<Transaction> transactions) throws IOException {
        Path absolute = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(absolute.getParent(), "expense-export-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write("ID,Type,Amount (INR),Category,Date,Description\r\n");
                for (Transaction transaction : transactions) {
                    writer.write(transaction.getId() + "," + cell(transaction.getType().toString()) + ","
                            + Money.decimal(transaction.getAmountPaise()) + "," + cell(transaction.getCategory())
                            + "," + transaction.getDate() + "," + cell(transaction.getDescription()) + "\r\n");
                }
            }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Escape RFC 4180 characters and prevent spreadsheet formula execution. */
    private static String cell(String value) {
        String stripped = value.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) value = "'" + value;
        if (value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
