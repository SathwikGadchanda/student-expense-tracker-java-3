package com.studentfinance.model;

public enum TransactionType {
    INCOME, EXPENSE;

    @Override
    public String toString() {
        return this == INCOME ? "Income" : "Expense";
    }
}
