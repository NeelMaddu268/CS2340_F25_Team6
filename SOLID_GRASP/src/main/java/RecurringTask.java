package src.main.java;

import java.util.Date;


public class RecurringTask extends Task {
    private int recurringDays;

    public RecurringTask(String title, String description, Date dueDate,
                         String status, int priority, int recurringDays) {
        super(title, description, dueDate, status, priority);
        this.recurringDays = recurringDays;
    }

    public int getRecurringDays() {
        return recurringDays;
    }

    public void setRecurringDays(int newRecurringDays) {
        this.recurringDays = newRecurringDays;
    }

    public String getRecurrenceType(int recurringDays) {
        return switch (recurringDays) {
        case 1 -> "Daily Task!";
        case 7 -> "Weekly Task!";
        case 14 -> "Bi-Weekly Task!";
        case 30 -> "Monthly Task!";
        default -> "The task returns every " + recurringDays + " days.";
        };
    }
}