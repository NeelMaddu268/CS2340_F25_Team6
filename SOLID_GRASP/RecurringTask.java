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
        if (recurringDays == 7) {
            return "Weekly Task!";
        } else if (recurringDays == 14) {
            return "Bi-weekly Task";
        } else if (recurringDays == 30) {
            return "Monthly Task";
        } else if (recurringDays == 1) {
            return "Daily Task";
        } else {
            return "The task returns every " + recurringDays + " days.";
        }
    }
}