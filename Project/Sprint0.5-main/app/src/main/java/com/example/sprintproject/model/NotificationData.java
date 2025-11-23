package com.example.sprintproject.model;

/**
 * Class for all in-app popup notifications.
 * Follows the "notification queue" requirement by unifying the data type
 * for both missed expense logs and budget warnings.
 */
public class NotificationData {
    public enum Type {
        MISSED_LOG,
        BUDGET_WARNING
    }

    private final Type type;
    private final String title;
    private final String message;
    private final int priority; // Higher number means higher priority

    /**
     * Creates a NotificationData object.
     * @param type The type of reminder
     * @param title The title of the pop-up.
     * @param message The detailed message to display.
     * @param priority The priority level
     */
    public NotificationData(Type type, String title, String message, int priority) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.priority = priority;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public int getPriority() {
        return priority;
    }

    public static NotificationData createMissedLogReminder(int days) {
        return new NotificationData(
                Type.MISSED_LOG,
                "Log Reminder",
                "It's been " + days + " days since your last expense!",
                100 // using a high priority for now
        );
    }
}