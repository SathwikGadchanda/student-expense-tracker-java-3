package com.studentfinance.dao;

import com.studentfinance.model.Transaction;
import java.sql.SQLException;
import java.util.List;

/** Abstraction keeps business logic independent of the SQLite implementation. */
public interface TransactionDao {
    List<Transaction> findAll() throws SQLException;
    Transaction insert(Transaction transaction) throws SQLException;
    void update(Transaction transaction) throws SQLException;
    void delete(long id) throws SQLException;
}
