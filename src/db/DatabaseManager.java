package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

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
}