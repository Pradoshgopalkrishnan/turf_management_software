package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import model.Admin;
import model.Customer;
import model.User;

/**
 * DatabaseManager: the ONLY class that talks to MySQL.
 * Every other class asks this class for data.
 *
 * In later phases we add methods here: validateUser, checkSlot,
 * saveBooking, validatePromoCode, and so on.
 * The URL, username and password are NOT written in the code.
 * They are read from a file called db.properties in the project root.
 * That file is listed in .gitignore, so it never goes to GitHub.
 */
public class DatabaseManager {

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();

        // Looks for db.properties in the folder you run the program from (the project root).
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(
                "Could not read db.properties in the project root. "
              + "Copy db.properties.example to db.properties and fill in your details.", e);
        }

        this.url      = props.getProperty("db.url");
        this.user     = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
    }

    /**
     * Opens a new connection to the database.
     * The caller must close it (use try-with-resources).
     */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // =====================================================================
    // Phase 4: Login and Register
    // =====================================================================

    /**
     * Looks for a user with this username AND password.
     * Returns a Customer or an Admin (depending on the 'role' column),
     * or null if no row matches.
     */
    public User validateUser(String username, String password) throws SQLException {
        String sql = "SELECT user_id, name, contact, username, password, role "
                   + "FROM users WHERE username = ? AND password = ?";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;   // wrong username or password
                }

                int id        = rs.getInt("user_id");
                String name   = rs.getString("name");
                String contact = rs.getString("contact");
                String uname  = rs.getString("username");
                String pass   = rs.getString("password");
                String role   = rs.getString("role");

                if ("ADMIN".equals(role)) {
                    return new Admin(id, name, contact, uname, pass);
                }
                return new Customer(id, name, contact, uname, pass);
            }
        }
    }

    /** Returns true if this username is already taken. */
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Inserts a new CUSTOMER account. (Admins are added directly in the database.) */
    public void registerUser(String name, String contact, String username, String password)
            throws SQLException {
        String sql = "INSERT INTO users (name, contact, username, password, role) "
                   + "VALUES (?, ?, ?, ?, 'CUSTOMER')";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setString(3, username);
            ps.setString(4, password);
            ps.executeUpdate();
        }
    }
}