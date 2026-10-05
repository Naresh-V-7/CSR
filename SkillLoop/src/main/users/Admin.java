package skillloop.users;

/**
 * The administrator of the platform.
 *
 * Admin exists mainly to make INHERITANCE and RUNTIME POLYMORPHISM real:
 * Student and Admin share the User code (id, name, email, login) but each one
 * shows a completely different dashboard.
 */
public class Admin extends User {

    private final String department;

    public Admin(String name, String email, String password, String department) {
        super(name, email, password);     // constructor inheritance
        this.department = department;
    }

    public Admin(String name, String email, String password) {
        this(name, email, password, "Platform Administration");
    }

    public String getDepartment() {
        return department;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    /**
     * METHOD OVERRIDING: the same method name as in Student, but a completely
     * different body. Java chooses which one to run at RUNTIME, based on the
     * object, not on the reference type.
     */
    @Override
    public void showDashboard() {
        System.out.println();
        System.out.println("============ ADMIN DASHBOARD ============");
        System.out.println(" Admin          : " + name + "  (ID " + id + ")");
        System.out.println(" Department     : " + department);
        System.out.println(" Platform       : " + APP_NAME + " v" + APP_VERSION);
        System.out.println(" Registered users: " + getTotalUsers());
        System.out.println("=========================================");
    }

    /** Admin-only action: used to show that the child can add its own methods. */
    public void printPlatformStats(int studentCount, int skillCount, int sessionCount) {
        System.out.println("Platform statistics -> students: " + studentCount
                + ", skills: " + skillCount + ", sessions: " + sessionCount);
    }
}
