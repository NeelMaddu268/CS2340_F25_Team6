package src.main.java;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TeamLeader implements Member {
    private static final Logger LOGGER = Logger.getLogger(TeamLeader.class.getName());
    private String name;
    private String email;

    public TeamLeader(String name, String email) {
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
        LOGGER.log(Level.INFO, "{0} is a Team Leader.", name);
    }

    @Override
    public void joinProject(Project project) {
        project.addTeamMember(this);
    }

    @Override
    public void leaveProject(Project project) {
        project.removeTeamMember(this);
    }

    public void provideFeedback(TeamMember member, String feedback) {
        LOGGER.log(Level.INFO, "Feedback for {0}: {1}", new Object[]{ member.getName(), feedback });    }
}