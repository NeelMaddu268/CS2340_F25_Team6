import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Project {
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;
    private List<Task> tasks = new ArrayList<>();
    private List<Member> members = new ArrayList<>();

    public Project(String name, Date startDate, Date endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void addTask(String title, String description,
                        Date dueDate, String status, int priority) {
        Task task = new Task(title, description, dueDate, status, priority);
        tasks.add(task);
    }


    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void addTeamMember(Member member) {
        members.add(member);
    }

    public void removeTeamMember(Member member) {
        members.remove(member);
    }

}
