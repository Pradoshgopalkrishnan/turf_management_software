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

import model.Admin;

/**
 * The main window an admin sees after logging in.
 *
 * Layout:
 *   top    : welcome message + Logout button
 *   centre : tabs  ->  Turfs | Bookings | Promo Codes | Pricing
 *
 * Phase 11 built the Turfs tab, Phase 12 the Bookings tab and Phase 13 the
 * Promo Codes and Pricing tabs.
 */
public class AdminDashboard extends JFrame {

    private final Admin admin;

    public AdminDashboard(Admin admin) {
        this.admin = admin;

        setTitle("Turf Booking System - Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);   // closing this window ends the program
        setSize(820, 620);
        setLocationRelativeTo(null);

        // ---- top bar: welcome text on the left, Logout on the right ----
        JLabel welcome = new JLabel("Welcome, " + admin.getName() + "!  (Admin)");
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
        ManageBookingsPanel bookingsPanel = new ManageBookingsPanel();
        tabs.addTab("Turfs", new ManageTurfsPanel());
        tabs.addTab("Bookings", bookingsPanel);
        tabs.addTab("Promo Codes", new ManagePromoPanel());
        tabs.addTab("Pricing", new ManagePricingPanel());

        // Reload the bookings whenever the admin opens that tab, so it also shows
        // turfs added meanwhile and bookings that customers made or cancelled.
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == bookingsPanel) {
                bookingsPanel.loadBookings();
            }
        });

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    /** Close this window and go back to the login screen. */
    private void handleLogout() {
        admin.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}