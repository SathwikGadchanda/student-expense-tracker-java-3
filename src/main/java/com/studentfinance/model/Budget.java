package com.studentfinance.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public record Budget(YearMonth month, long limitPaise) {
    public Budget {
        Objects.requireNonNull(month, "Choose a month.");
        if (month.getYear() < 0 || month.getYear() > 9999)
            throw new IllegalArgumentException("Use a four-digit year.");
        if (limitPaise < 1 || limitPaise > Transaction.MAX_AMOUNT_PAISE)
            throw new IllegalArgumentException("Enter a limit between INR 0.01 and INR 10,00,000.00.");
    }

    public long remaining(long spent) { return limitPaise - spent; }

    public String status(long spent) {
        if (spent > limitPaise) return "Budget exceeded";
        if (spent == limitPaise) return "Budget reached";
        BigDecimal threshold = BigDecimal.valueOf(limitPaise).multiply(new BigDecimal("0.80"));
        if (BigDecimal.valueOf(spent).compareTo(threshold) >= 0) return "80% warning";
        return "Within budget";
    }
}
