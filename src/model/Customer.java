package model;

/**
 * A Customer IS-A User (inheritance).
 * It fills in the abstract methods from User in its own way (polymorphism).
 */
public class Customer extends User {

    public Customer(int userId, String name, String contact, String username, String password) {
        super(userId, name, contact, username, password);   // pass data up to User
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    @Override
    public void openDashboard() {
        new ui.CustomerDashboard(this).setVisible(true);
    }
}