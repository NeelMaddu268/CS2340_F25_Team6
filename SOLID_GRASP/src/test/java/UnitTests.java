import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.util.Date;

public class UnitTests {
    @Test
    public void testSetup() {
        Project project1 = new Project("Project 1", new Date(), new Date());
        Project project2 = new Project("Project 2", new Date(), new Date());

        TeamLeader leader1 = new TeamLeader("Leader 1", "leader1@gmail.com");
        TeamLeader leader2 = new TeamLeader("Leader 2", "leader2@gmail.com");

        TeamMember member1 = new TeamMember("Member 1", "member1@gmail.com");
        TeamMember member2 = new TeamMember("Member 2", "member2@gmail.com");

        assertEquals(0, project1.getMembers().size());
        assertEquals(0, project2.getMembers().size());

        leader1.joinProject(project1);
        member1.joinProject(project1);
        member2.joinProject(project1);

        assertEquals(3, project1.getMembers().size());

        member2.leaveProject(project1);
        assertEquals(2, project1.getMembers().size());

        leader2.joinProject(project2);
        assertEquals(1, project2.getMembers().size());
    }

    @Test
    public void testTaskCreation() {
        Task task1 = new Task("Task 1", "Description 1", new Date(), "To Do", 1);
        Task task2 = new Task("Task 2", "Description 2", new Date(), "Almost Done", 2);

        assertEquals("Task 1", task1.getTitle());
        assertEquals("Description 1", task1.getDescription());
        assertEquals("To Do", task1.getStatus());
        assertEquals(1, task1.getPriority());

        assertEquals("Task 2", task2.getTitle());
        assertEquals("Almost Done", task2.getStatus());
        assertEquals(2, task2.getPriority());
    }

    @Test
    public void testUpdateTaskStatus() {
        Task task = new Task("Task 1", "Description 1", new Date(), "To Do", 1);
        TeamMember member = new TeamMember("Member 1", "member1@gmail.com");

        member.updateTaskStatus(task, "In Progress");
        assertEquals("In Progress", task.getStatus());
    }

    @Test
    public void testEditTaskInfo() {
        Task task = new Task("Task 2", "Description 2", new Date(), "Almost Done", 2);

        task.setTitle("Task 2 Updated");
        task.setPriority(1);
        task.setStatus("Blocked");

        assertEquals("Task 2 Updated", task.getTitle());
        assertEquals("Blocked", task.getStatus());
        assertEquals(1, task.getPriority());
    }

    @Test
    public void testAddTasksToProject() {
        Project project = new Project("Project 1", new Date(), new Date());
        assertEquals(0, project.getTasks().size());

        project.addTask("Task 1", "Description 1", new Date(), "To Do", 1);
        project.addTask("Task 2", "Description 2", new Date(), "In Progress", 2);

        assertEquals(2, project.getTasks().size());
        assertEquals("Task 1", project.getTasks().get(0).getTitle());
        assertEquals("Task 2", project.getTasks().get(1).getTitle());
    }
}
