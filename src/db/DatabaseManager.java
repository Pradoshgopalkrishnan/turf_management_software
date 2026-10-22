package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import model.Admin;
import model.Booking;
import model.Customer;
import model.PromoCode;
import model.Turf;
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

    // =====================================================================
    // Phase 5: Turfs
    // =====================================================================

    /** Returns every turf in the database, ordered by name. */
    public List<Turf> getAllTurfs() throws SQLException {
        String sql = "SELECT turf_id, name, location, capacity, base_price "
                   + "FROM turfs ORDER BY name";

        List<Turf> turfs = new ArrayList<>();

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                turfs.add(new Turf(
                        rs.getInt("turf_id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getInt("capacity"),
                        rs.getDouble("base_price")));
            }
        }
        return turfs;
    }

    // =====================================================================
    // Phase 6: Slot availability
    // =====================================================================

    /**
     * Is this slot free?  Returns true when there is NO confirmed booking for
     * the given turf + date + time slot.  Cancelled bookings do not block a slot.
     */
    public boolean checkSlot(int turfId, java.sql.Date date, String timeSlot) throws SQLException {
        try (Connection con = connect()) {
            return isSlotFree(con, turfId, date, timeSlot);
        }
    }

    /**
     * The real slot check. It works on a connection that is passed in, so
     * saveBooking() (Phase 9) can run the same check INSIDE its transaction.
     */
    private boolean isSlotFree(Connection con, int turfId, java.sql.Date date, String timeSlot)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings "
                   + "WHERE turf_id = ? AND booking_date = ? "
                   + "AND time_slot = ? AND status = 'CONFIRMED'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, turfId);
            ps.setDate(2, date);
            ps.setString(3, timeSlot);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();                       // COUNT(*) always returns exactly one row
                return rs.getInt(1) == 0;        // 0 bookings -> slot is free
            }
        }
    }

    // =====================================================================
    // Phase 7: Pricing
    // =====================================================================

    /**
     * Reads one surge multiplier from pricing_rules ("WEEKEND" or "EVENING").
     * Returns 1.0 (no surge) if the rule does not exist.
     */
    public double getMultiplier(String ruleName) throws SQLException {
        String sql = "SELECT multiplier FROM pricing_rules WHERE rule_name = ?";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, ruleName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("multiplier");
                }
                return 1.0;                      // rule not found -> no surge
            }
        }
    }

    // =====================================================================
    // Phase 8: Promo codes
    // =====================================================================

    /**
     * Looks up an ACTIVE promo code. Returns a PromoCode object if one is found,
     * otherwise null. The caller must still check promo.isValid() for the expiry date.
     * (MySQL compares text without caring about upper/lower case, so "welcome10" works.)
     */
    public PromoCode validatePromoCode(String code) throws SQLException {
        String sql = "SELECT code, discount_percent, expiry_date, is_active "
                   + "FROM promo_codes WHERE code = ? AND is_active = TRUE";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PromoCode(
                            rs.getString("code"),
                            rs.getDouble("discount_percent"),
                            rs.getDate("expiry_date").toLocalDate(),
                            rs.getBoolean("is_active"));
                }
                return null;                     // no active code with that name
            }
        }
    }

    // =====================================================================
    // Phase 9: Save a booking
    // =====================================================================

    /**
     * Saves a booking. Returns the new booking ID, or -1 if the slot was
     * taken by somebody else in the meantime (nothing is saved in that case).
     *
     * Why a transaction? Checking "is it free?" and then inserting are TWO
     * steps. If two customers click Confirm at the same moment, both could pass
     * the check before either one inserts, and the slot would be booked twice.
     * So we do everything in one transaction and first LOCK the turf's row
     * (SELECT ... FOR UPDATE). A second customer booking the same turf waits
     * here until the first one has committed, and then their check sees the
     * first booking and says "taken".
     *
     * Steps:  1. lock the turf row
     *         2. re-run the slot check (the same check as checkSlot)
     *         3. INSERT the booking
     *         4. commit  (or rollback if anything goes wrong)
     */
    public int saveBooking(Booking b) throws SQLException {
        String lockSql = "SELECT turf_id FROM turfs WHERE turf_id = ? FOR UPDATE";
        String insertSql = "INSERT INTO bookings "
                + "(user_id, turf_id, booking_date, time_slot, base_price, surge_multiplier, "
                + "promo_code, discount_amount, final_total, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED')";

        java.sql.Date date = java.sql.Date.valueOf(b.getBookingDate());

        try (Connection con = connect()) {
            con.setAutoCommit(false);              // start the transaction
            try {
                // 1. lock this turf's row (waits if another booking for it is in progress)
                try (PreparedStatement lock = con.prepareStatement(lockSql)) {
                    lock.setInt(1, b.getTurfId());
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("This turf no longer exists.");
                        }
                    }
                }

                // 2. re-check the slot while holding the lock
                if (!isSlotFree(con, b.getTurfId(), date, b.getTimeSlot())) {
                    con.rollback();
                    return -1;                     // somebody else got it first
                }

                // 3. insert the booking and read back the ID MySQL generated
                try (PreparedStatement ps =
                         con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, b.getUserId());
                    ps.setInt(2, b.getTurfId());
                    ps.setDate(3, date);
                    ps.setString(4, b.getTimeSlot());
                    ps.setDouble(5, b.getBasePrice());
                    ps.setDouble(6, b.getSurgeMultiplier());
                    if (b.getPromoCode() == null) {
                        ps.setNull(7, Types.VARCHAR);   // no promo used -> NULL in the table
                    } else {
                        ps.setString(7, b.getPromoCode());
                    }
                    ps.setDouble(8, b.getDiscountAmount());
                    ps.setDouble(9, b.getFinalTotal());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        b.setBookingId(keys.getInt(1));
                    }
                }

                // 4. make it permanent (this also releases the lock)
                con.commit();
                return b.getBookingId();

            } catch (SQLException e) {
                con.rollback();                    // undo everything, then tell the caller
                throw e;
            }
        }
    }

    // =====================================================================
    // Phase 10: My Bookings and Cancel
    // =====================================================================

    /**
     * Returns every booking made by this user (confirmed AND cancelled), newest first,
     * each with its turf name filled in.
     */
    public List<Booking> getBookingsByUser(int userId) throws SQLException {
        String sql = "SELECT b.booking_id, b.user_id, b.turf_id, t.name AS turf_name, "
                   + "b.booking_date, b.time_slot, b.base_price, b.surge_multiplier, "
                   + "b.promo_code, b.discount_amount, b.final_total, b.status "
                   + "FROM bookings b JOIN turfs t ON b.turf_id = t.turf_id "
                   + "WHERE b.user_id = ? "
                   + "ORDER BY b.booking_id DESC";   // highest ID = booked most recently

        List<Booking> bookings = new ArrayList<>();

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking(
                            rs.getInt("booking_id"),
                            rs.getInt("user_id"),
                            rs.getInt("turf_id"),
                            rs.getDate("booking_date").toLocalDate(),
                            rs.getString("time_slot"),
                            rs.getDouble("base_price"),
                            rs.getDouble("surge_multiplier"),
                            rs.getString("promo_code"),      // null when no promo was used
                            rs.getDouble("discount_amount"),
                            rs.getDouble("final_total"),
                            rs.getString("status"));
                    b.setTurfName(rs.getString("turf_name"));
                    bookings.add(b);
                }
            }
        }
        return bookings;
    }

    /**
     * Cancels a booking. Returns true if it was cancelled, false if nothing changed.
     * The WHERE clause has three safety checks:
     *   booking_id           -> the right booking
     *   user_id              -> a customer can only cancel THEIR OWN booking
     *   status = 'CONFIRMED' -> an already-cancelled booking is not cancelled twice
     * The booking row is kept (status becomes CANCELLED), so the history stays, and
     * checkSlot() ignores cancelled rows, so the slot becomes free again.
     */
    public boolean cancelBooking(int bookingId, int userId) throws SQLException {
        String sql = "UPDATE bookings SET status = 'CANCELLED' "
                   + "WHERE booking_id = ? AND user_id = ? AND status = 'CONFIRMED'";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;      // 1 row changed = cancelled
        }
    }
}