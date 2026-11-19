package com.studentfinance.model;

import java.time.LocalDate;

/** Immutable value object. Monetary amounts use paise, never binary floating point. */
public final class Transaction {
    public static final long MAX_AMOUNT_PAISE = 100_000_000L;
    private final long id;
    private final TransactionType type;
    private final long amountPaise;
    private final String category;
    private final LocalDate date;
    private final String description;

    public Transaction(long id, TransactionType type, long amountPaise,
                       String category, LocalDate date, String description) {
        if (id < 0) throw new IllegalArgumentException("Invalid transaction ID.");
        if (type == null || date == null) throw new IllegalArgumentException("Type and date are required.");
        if (amountPaise <= 0 || amountPaise > MAX_AMOUNT_PAISE)
            throw new IllegalArgumentException("Amount must be between INR 0.01 and INR 10,00,000.00.");
        if (category == null || !Category.forType(type).contains(category))
            throw new IllegalArgumentException("Choose a valid category for this transaction type.");
        String text = description == null ? "" : description.strip();
        if (text.length() > 250) throw new IllegalArgumentException("Description must be 250 characters or fewer.");
        this.id = id;
        this.type = type;
        this.amountPaise = amountPaise;
        this.category = category;
        this.date = date;
        this.description = text;
    }

    public long getId() { return id; }
    public TransactionType getType() { return type; }
    public long getAmountPaise() { return amountPaise; }
    public String getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }

    public Transaction withId(long newId) {
        return new Transaction(newId, type, amountPaise, category, date, description);
    }
}
