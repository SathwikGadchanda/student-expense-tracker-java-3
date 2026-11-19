package com.studentfinance.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() { }

    public static long parsePaise(String input) {
        try {
            // Plain decimal input prevents ambiguous grouping, exponents and silent rounding.
            if (input == null || !input.strip().matches("[0-9]+(\\.[0-9]{1,2})?"))
                throw new NumberFormatException();
            return new BigDecimal(input.strip()).setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException("Enter an amount such as 150 or 150.50, with at most two decimal places.");
        }
    }

    public static String decimal(long paise) { return BigDecimal.valueOf(paise, 2).toPlainString(); }

    public static String format(long paise) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "INR " + format.format(BigDecimal.valueOf(paise, 2));
    }
}
