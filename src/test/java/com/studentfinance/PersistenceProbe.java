package com.studentfinance;

import com.studentfinance.dao.SqliteTransactionDao;
import com.studentfinance.database.Database;
import com.studentfinance.model.*;
import com.studentfinance.service.FinanceService;
import java.nio.file.Path;
import java.time.LocalDate;

/** Invoke in two separate JVMs to verify data survives process exit. */
public final class PersistenceProbe {
    public static void main(String[] args) throws Exception {
        Database database = new Database(Path.of(args[1])); database.initialize();
        FinanceService service = new FinanceService(new SqliteTransactionDao(database));
        if (args[0].equals("write")) {
            service.save(new Transaction(0, TransactionType.INCOME, 500000, "Pocket Money", LocalDate.of(2026, 11, 18), "Restart test"));
            System.out.println("WRITE_OK");
        } else {
            if (service.all().size() != 1 || service.all().get(0).getAmountPaise() != 500000)
                throw new AssertionError("Persistence verification failed");
            System.out.println("RESTART_READ_OK");
        }
    }
}
