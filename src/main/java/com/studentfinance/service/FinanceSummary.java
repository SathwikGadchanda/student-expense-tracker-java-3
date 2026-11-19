package com.studentfinance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public record FinanceSummary(long totalIncome, long totalExpenses, long balance,
                             long monthlyIncome, long monthlyExpenses, YearMonth month,
                             Map<String, Long> categoryExpenses, Map<LocalDate, Long> dailyExpenses,
                             Map<YearMonth, Long> monthlyHistory) {
    /** Monday identifies a week. Only days inside the selected month contribute. */
    public Map<LocalDate, Long> weeklyExpenses() {
        Map<LocalDate, Long> weeks = new java.util.TreeMap<>();
        dailyExpenses.forEach((day, amount) -> {
            LocalDate monday = day.with(java.time.DayOfWeek.MONDAY);
            weeks.merge(monday, amount, Math::addExact);
        });
        return java.util.Collections.unmodifiableMap(weeks);
    }
}
