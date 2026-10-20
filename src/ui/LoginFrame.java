package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import db.DatabaseManager;
import model.User;

/**
 * The first window the user sees.
 * Flow: type username + password -> Login -> validateUser() -> open the right dashboard.
 */
public class LoginFrame extends JFrame {

    private final DatabaseManager db = new DatabaseManager();

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Turf Booking System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);   // closing this window ends the program
        setSize(380, 260);
        setResizable(false);
        setLocationRelativeTo(null);               // centre on screen

        // ---- title at the top ----
        JLabel title = new JLabel("Turf Booking System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        // ---- the two input rows in the middle ----
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 15));   // 2 rows, 2 columns
        form.setBorder(BorderFactory.createEmptyBorder(15, 30, 10, 30));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);

        // ---- buttons at the bottom ----
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttons.add(loginButton);
        buttons.add(registerButton);

        // ---- put the three parts into the window ----
        setLayout(new BorderLayout());
        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // ---- what happens when buttons are clicked ----
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> new RegisterFrame().setVisible(true));

        getRootPane().setDefaultButton(loginButton);   // pressing Enter clicks Login
    }

    /** Runs when the Login button is clicked. */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // 1. empty-field check
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 2. ask the database
            User user = db.validateUser(username, password);

            // 3. wrong details
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Login failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                return;
            }

            // 4. correct details: open the right dashboard
            user.login();
            user.openDashboard();   // POLYMORPHISM: Customer and Admin each do their own thing
            new PlaceholderDashboard(user).setVisible(true);
            dispose();              // close the login window

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to the database.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
