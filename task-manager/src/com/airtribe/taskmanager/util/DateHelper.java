package com.airtribe.taskmanager.util;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Date helpers. Dates are always plain strings in YYYY-MM-DD format.
 *
 * <p>{@link #isValidDate(String)} checks the month and day by hand (including leap years),
 * so "2026-02-30" and "2026-02-29" are rejected but "2028-02-29" is accepted.
 * The other methods use {@code java.time.LocalDate} for date arithmetic.
 */
public final class DateHelper {

    private DateHelper() {
    }

    /** True when the text is a real calendar date written exactly as YYYY-MM-DD. */
    public static boolean isValidDate(String text) {
        if (text == null || text.length() != 10 || text.charAt(4) != '-' || text.charAt(7) != '-') {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (i != 4 && i != 7 && !Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        int year = Integer.parseInt(text.substring(0, 4));
        int month = Integer.parseInt(text.substring(5, 7));
        int day = Integer.parseInt(text.substring(8, 10));
        if (year < 1 || month < 1 || month > 12 || day < 1) {
            return false;
        }
        return day <= daysInMonth(year, month);
    }

    /** Gregorian leap year: every 4th year, except centuries, except every 400th year. */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    public static int daysInMonth(int year, int month) {
        switch (month) {
            case 2:
                return isLeapYear(year) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    /** Today's date from the given clock, as YYYY-MM-DD. */
    public static String today(Clock clock) {
        return LocalDate.now(clock).toString();
    }

    /** The date {@code days} days after {@code isoDate}. */
    public static String plusDays(String isoDate, int days) {
        return LocalDate.parse(isoDate).plusDays(days).toString();
    }

    /** Day of the week for a date, for example "FRIDAY". */
    public static String dayOfWeek(String isoDate) {
        return LocalDate.parse(isoDate).getDayOfWeek().name();
    }

    /**
     * The first date on or after {@code today} that falls on the given day of the month.
     * Months that are too short are skipped, so the 31st after 2026-11-27 is 2026-12-31.
     */
    public static String nextDayOfMonth(String today, int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("day of month must be 1-31, got " + dayOfMonth);
        }
        LocalDate start = LocalDate.parse(today);
        LocalDate month = start.withDayOfMonth(1);
        for (int i = 0; i < 13; i++) {
            if (dayOfMonth <= month.lengthOfMonth()) {
                LocalDate candidate = month.withDayOfMonth(dayOfMonth);
                if (!candidate.isBefore(start)) {
                    return candidate.toString();
                }
            }
            month = month.plusMonths(1);
        }
        throw new IllegalStateException("no month found for day " + dayOfMonth);
    }

    /** The first date strictly after {@code today} that falls on the given weekday. */
    public static String nextWeekday(String today, DayOfWeek weekday) {
        LocalDate date = LocalDate.parse(today).plusDays(1);
        while (date.getDayOfWeek() != weekday) {
            date = date.plusDays(1);
        }
        return date.toString();
    }
}
