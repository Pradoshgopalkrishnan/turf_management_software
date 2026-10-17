package model;

/**
 * User is ABSTRACT: you can never write "new User(...)".
 * You can only create a Customer or an Admin, which both EXTEND User.
 *
 * OOP concepts shown here:
 *  - Abstraction   : the class is abstract and has abstract methods
 *  - Encapsulation : fields are private; other classes use getters
 */
public abstract class User {

    private int userId;
    private String name;
    private String contact;
    private String username;
    private String password;
    private boolean loggedIn;

    public User(int userId, String name, String contact, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.contact = contact;
        this.username = username;
        this.password = password;
        this.loggedIn = false;
    }

    // ---- abstract methods: every subclass MUST write its own version ----
    public abstract void openDashboard();   // polymorphism: each role opens a different window
    public abstract String getRole();       // "ADMIN" or "CUSTOMER"

    // ---- normal methods shared by every user ----
    public void login()  { loggedIn = true; }
    public void logout() { loggedIn = false; }
    public boolean isLoggedIn() { return loggedIn; }

    // ---- getters (no setter for userId: it should never change) ----
    public int getUserId()      { return userId; }
    public String getName()     { return name; }
    public String getContact()  { return contact; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
