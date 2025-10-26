import java.util.Date;

public class Task {
    private String title;
    private String description;
    private Date dueDate;
    private String status;
    private int priority;

    public Task(String title, String description, Date dueDate, String status, int priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public void setDueDate(Date newDueDate) {
        this.dueDate = newDueDate;
    }

    public void setStatus(String newStatus) {
        this.status = newStatus;
    }

    public void setPriority(int newPriority) {
        this.priority = newPriority;
    }
}
