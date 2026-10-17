package db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Throwaway test for Phases 1 and 2.
 * Run it once to confirm Java can reach MySQL and read the seeded turfs.
 * You can delete this file after it works.
 */
public class Testconnection {

    public static void main(String[] args) {
        DatabaseManager dbm = new DatabaseManager();

        // try-with-resources closes everything automatically
        try (Connection con = dbm.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT turf_id, name, base_price FROM turfs")) {

            System.out.println("Connected!");
            while (rs.next()) {
                System.out.println(rs.getInt("turf_id") + " | "
                        + rs.getString("name") + " | Rs. "
                        + rs.getDouble("base_price"));
            }

        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}