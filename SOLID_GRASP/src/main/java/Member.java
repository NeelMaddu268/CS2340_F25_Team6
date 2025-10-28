public interface Member {
    String getName();
    String getEmail();
    void identify();
    void joinProject(Project project);
    void leaveProject(Project project);
}
