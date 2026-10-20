package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import model.User;

/**
 * TEMPORARY window shown after a successful login, so we can SEE that login works.
 * Phase 5 replaces it for customers (CustomerDashboard) and Phase 11 for admins
 * (AdminDashboard). Then this file gets deleted.
 */
public class PlaceholderDashboard extends JFrame {

    public PlaceholderDashboard(User user) {
        setTitle(user.getRole() + " Dashboard (placeholder)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 240);
        setLocationRelativeTo(null);

        JLabel welcome = new JLabel("Welcome, " + user.getName() + "!", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel role = new JLabel("Logged in as: " + user.getRole(), SwingConstants.CENTER);
        JLabel note = new JLabel("(The real dashboard is built in a later phase.)", SwingConstants.CENTER);

        JPanel center = new JPanel(new GridLayout(3, 1));
        center.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        center.add(welcome);
        center.add(role);
        center.add(note);

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            user.logout();
            dispose();
            new LoginFrame().setVisible(true);   // back to the login window
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        bottom.add(logout);

        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}
