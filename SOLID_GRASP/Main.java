import org.junit.Test;

import java.util.Date;
import java.util.logging.Logger;


public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    @Test
    public void testMain() {

        Project project1 = new Project("Project 1", new Date(), new Date());
        Project project2 = new Project("Project 2", new Date(), new Date());

        TeamLeader leader1 = new TeamLeader("Leader 1", "leader1@gmail.com");
        TeamLeader leader2 = new TeamLeader("Leader 2", "leader2@gmail.com");

        TeamMember member1 = new TeamMember("Member 1", "member1@gmail.com");
        TeamMember member2 = new TeamMember("Member 2", "member2@gmail.com");

        logger.info("Members in Project 1 before:");
        logger.info("No members yet.\n");

        leader1.joinProject(project1);
        member1.joinProject(project1);
        member2.joinProject(project1);

        logger.info("Members in Project 1 after joining:");
        leader1.identify();
        member1.identify();
        member2.identify();

        member2.leaveProject(project1);

        logger.info("Members in Project 1 after one left:");
        leader1.identify();
        member1.identify();

        logger.info("Members in Project 2 before:");
        logger.info("No members yet.\n");

        leader2.joinProject(project2);

        logger.info("Members in Project 2 after joining:");
        leader2.identify();

        Task task1 = new Task("Task 1", "Description 1", new Date(), "To Do", 1);
        Task task2 = new Task("Task 2", "Description 2", new Date(), "Almost Done", 2);
        Task task3 = new Task("Task 3", "Description 3", new Date(), "Complete", 3);

        logger.info("Tasks:");
        logger.info(task1.getTitle() + ", " + task1.getStatus()
                + ", " + task1.getPriority());
        logger.info(task2.getTitle() + ", " + task2.getStatus()
                + ", " + task2.getPriority());
        logger.info(task3.getTitle() + ", " + task3.getStatus()
                + ", " + task3.getPriority());
        
        member1.updateTaskStatus(task1, "In Progress");
        logger.info(task1.getTitle() + " -> "
                + task1.getStatus() + "\n");

        logger.info("Tasks after update:");
        logger.info(task1.getTitle() + ", "
                + task1.getStatus() + ", " + task1.getPriority());
        logger.info(task2.getTitle() + ", "
                + task2.getStatus() + ", " + task2.getPriority());
        logger.info(task3.getTitle() + ", "
                + task3.getStatus() + ", " + task3.getPriority());
    }
}
