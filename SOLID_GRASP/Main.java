import java.util.Date;

public class Main {
    public static void main(String[] args) {
        Project project1 = new Project("Project 1", new Date(), new Date());
        Project project2 = new Project("Project 2", new Date(), new Date());

        TeamLeader leader1 = new TeamLeader("Leader 1", "leader1@gmail.com");
        TeamLeader leader2 = new TeamLeader("Leader 2", "leader2@gmail.com");

        TeamMember member1 = new TeamMember("Member 1", "member1@gmail.com");
        TeamMember member2 = new TeamMember("Member 2", "member2@gmail.com");

        System.out.println("Members in Project 1 before:");
        System.out.println("No members yet.\n");

        leader1.joinProject(project1);
        member1.joinProject(project1);
        member2.joinProject(project1);

        System.out.println("Members in Project 1 after joining:");
        leader1.identify();
        member1.identify();
        member2.identify();
        System.out.println();

        member2.leaveProject(project1);

        System.out.println("Members in Project 1 after one left:");
        leader1.identify();
        member1.identify();
        System.out.println();

        System.out.println("Members in Project 2 before:");
        System.out.println("No members yet.\n");

        leader2.joinProject(project2);

        System.out.println("Members in Project 2 after joining:");
        leader2.identify();
        System.out.println();

        Task task1 = new Task("Task 1", "Description 1", new Date(), "To Do", 1);
        Task task2 = new Task("Task 2", "Description 2", new Date(), "To Do", 2);
        Task task3 = new Task("Task 3", "Description 3", new Date(), "To Do", 3);

        System.out.println("Tasks:");
        System.out.println(task1.getTitle() + ", " + task1.getStatus()
                + ", Priority " + task1.getPriority());
        System.out.println(task2.getTitle() + ", " + task2.getStatus()
                + ", Priority " + task2.getPriority());
        System.out.println(task3.getTitle() + ", " + task3.getStatus()
                + ", Priority " + task3.getPriority());
        System.out.println();

        member1.updateTaskStatus(task1, "In Progress");
        System.out.println(task1.getTitle() + " -> "
                + task1.getStatus() + "\n");

        System.out.println("Tasks after update:");
        System.out.println(task1.getTitle() + ", "
                + task1.getStatus() + ", Priority " + task1.getPriority());
        System.out.println(task2.getTitle() + ", "
                + task2.getStatus() + ", Priority " + task2.getPriority());
        System.out.println(task3.getTitle() + ", "
                + task3.getStatus() + ", Priority " + task3.getPriority());
        System.out.println();
    }
}
