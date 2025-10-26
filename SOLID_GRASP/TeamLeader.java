public class TeamLeader implements Member {
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
        System.out.println(name + " is a Team Member.");
    }

    @Override
    public void joinProject(Project project) {
        project.addTeamMember(this);
    }

    @Override
    public void leaveProject(Project project) {
        project.removeTeamMember(this);
    }
}