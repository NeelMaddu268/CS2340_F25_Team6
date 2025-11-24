package src.main.java;

import java.util.logging.Logger;

public class TeamMember implements Member {

    private static final Logger LOGGER = Logger.getLogger(TeamMember.class.getName());

    private String name;
    private String email;

    public TeamMember(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void identify() {
        LOGGER.info("Team Member: " + name);
    }

    @Override
    public void joinProject(Project project) {
        // Add team member to project
        project.addTeamMember(this);
    }

    @Override
    public void leaveProject(Project project) {
        project.removeTeamMember(this);
    }

    public void updateTaskStatus(Task task, String newStatus) {
        task.setStatus(newStatus);
    }
}
