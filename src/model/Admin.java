package model;

/**
 * An Admin IS-A User (inheritance).
 * Same method names as Customer, different behaviour (polymorphism).
 */
public class Admin extends User {

    public Admin(int userId, String name, String contact, String username, String password) {
        super(userId, name, contact, username, password);
    }

    @Override //OVERRIDE HELPS US IN MAKING SURE THAT THE METHOD NAMES ARE NOT DIFFERENT WHICH CAUSE ERROR 
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public void openDashboard() {
        // Phase 11 will replace this line with: new AdminDashboard(this).setVisible(true);
        System.out.println("Opening ADMIN dashboard for " + getName());
    }
}