package com.airtribe.taskmanager.parser;

import com.airtribe.taskmanager.util.DateHelper;

import java.time.DayOfWeek;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Works offline: understands a few common phrases with String methods and simple patterns.
 *
 * <ul>
 *   <li>"today", "tomorrow" -> due date</li>
 *   <li>"1st", "2nd", "15th" ... -> the next date with that day of the month</li>
 *   <li>"monday" ... "sunday" -> the next date on that weekday</li>
 *   <li>"every day/week/month/year", "daily", "weekly", "monthly", "yearly" -> recurrence</li>
 * </ul>
 * The title is what is left after removing "remind me to" and the date and repeat words.
 * The description is the original sentence.
 *
 * <p>Example: "Remind me to pay rent on the 1st of every month" on 2026-11-27 gives title
 * "Pay rent", dueDate "2026-12-01", recurrence MONTHLY.
 */
public class RuleBasedTaskParser implements TaskParser {

    private static final Pattern DAY_OF_MONTH = Pattern.compile("\\b(\\d{1,2})(st|nd|rd|th)\\b", Pattern.CASE_INSENSITIVE);

    /** Phrases removed from the title, in this order. */
    private static final String[] NOISE = {
            "(?i)^\\s*(please\\s+)?(remind me to|remind me|i need to|i have to|i must|don'?t forget to)\\s+",
            "(?i)\\b(on\\s+)?(the\\s+)?\\d{1,2}(st|nd|rd|th)\\b",
            "(?i)\\b(of\\s+|on\\s+)?every\\s+(day|week|month|year)\\b",
            "(?i)\\b(daily|weekly|monthly|yearly|annually)\\b",
            "(?i)\\b(by\\s+|on\\s+|next\\s+|every\\s+)*(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
            "(?i)\\b(by\\s+|on\\s+)?(tomorrow|today)\\b",
    };

    @Override
    public ParsedTask parse(String text, String today) {
        String lower = text.toLowerCase(Locale.ROOT);
        String recurrence = findRecurrence(lower);
        String dueDate = findDueDate(lower, today);
        return new ParsedTask(buildTitle(text), text.trim(), dueDate, recurrence);
    }

    private static String findRecurrence(String lower) {
        if (lower.contains("every day") || lower.contains("daily")) {
            return "DAILY";
        }
        if (lower.contains("every week") || lower.contains("weekly")) {
            return "WEEKLY";
        }
        if (lower.contains("every month") || lower.contains("monthly")) {
            return "MONTHLY";
        }
        if (lower.contains("every year") || lower.contains("yearly") || lower.contains("annually")) {
            return "YEARLY";
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            if (lower.contains("every " + day.name().toLowerCase(Locale.ROOT))) {
                return "WEEKLY";
            }
        }
        return null;
    }

    private static String findDueDate(String lower, String today) {
        if (lower.contains("tomorrow")) {
            return DateHelper.plusDays(today, 1);
        }
        if (lower.contains("today")) {
            return today;
        }
        Matcher matcher = DAY_OF_MONTH.matcher(lower);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            // An impossible day such as "45th" is passed on as an impossible date; validation rejects it.
            return (day >= 1 && day <= 31) ? DateHelper.nextDayOfMonth(today, day) : today.substring(0, 8) + day;
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            if (lower.contains(day.name().toLowerCase(Locale.ROOT))) {
                return DateHelper.nextWeekday(today, day);
            }
        }
        return null;
    }

    private static String buildTitle(String text) {
        String title = text;
        for (String pattern : NOISE) {
            title = title.replaceAll(pattern, " ");
        }
        title = title.replaceAll("\\s+", " ").trim();
        // Drop punctuation left at the end, such as "Pay rent ." or "Call mom,".
        while (!title.isEmpty() && ".,;:!".indexOf(title.charAt(title.length() - 1)) >= 0) {
            title = title.substring(0, title.length() - 1).trim();
        }
        if (title.isEmpty()) {
            return title; // validation will report "title must not be empty"
        }
        return Character.toUpperCase(title.charAt(0)) + title.substring(1);
    }
}
