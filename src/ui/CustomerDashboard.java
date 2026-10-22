package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import model.Customer;

/**
 * The main window a customer sees after logging in.
 *
 * Layout:
 *   top    : welcome message + Logout button
 *   centre : tabs  ->  "Book a Slot"  |  "My Bookings"
 */
public class CustomerDashboard extends JFrame {

    private final Customer customer;

    public CustomerDashboard(Customer customer) {
        this.customer = customer;

        setTitle("Turf Booking System - Customer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);   // closing this window ends the program
        setSize(720, 720);
        setLocationRelativeTo(null);

        // ---- top bar: welcome text on the left, Logout on the right ----
        JLabel welcome = new JLabel("Welcome, " + customer.getName() + "!");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 16));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> handleLogout());

        JPanel logoutHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        logoutHolder.add(logoutButton);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        topBar.add(welcome, BorderLayout.WEST);
        topBar.add(logoutHolder, BorderLayout.EAST);

        // ---- the tabs ----
        JTabbedPane tabs = new JTabbedPane();
        BookSlotPanel bookPanel = new BookSlotPanel(customer);
        MyBookingsPanel bookingsPanel = new MyBookingsPanel(customer);
        tabs.addTab("Book a Slot", bookPanel);
        tabs.addTab("My Bookings", bookingsPanel);

        // Reload whichever tab the customer switches to, so the two tabs never show
        // stale data (a new booking appears in My Bookings, a cancelled slot is free again).
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == bookingsPanel) {
                bookingsPanel.loadBookings();
            } else {
                bookPanel.refreshSlots();
            }
        });

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    /** Close this window and go back to the login screen. */
    private void handleLogout() {
        customer.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}