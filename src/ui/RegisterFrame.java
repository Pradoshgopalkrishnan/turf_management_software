package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import db.DatabaseManager;

/**
 * Lets a new customer create an account.
 * Admin accounts are NOT created here; they are added directly in the database.
 */
public class RegisterFrame extends JFrame {

    private final DatabaseManager db = new DatabaseManager();

    private JTextField nameField;
    private JTextField contactField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;

    public RegisterFrame() {
        setTitle("Register New Customer");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);   // closing this only closes THIS window
        setSize(400, 340);
        setResizable(false);
        setLocationRelativeTo(null);

        nameField     = new JTextField();
        contactField  = new JTextField();
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        confirmField  = new JPasswordField();

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        form.add(new JLabel("Full name:"));         form.add(nameField);
        form.add(new JLabel("Contact (10 digits):")); form.add(contactField);
        form.add(new JLabel("Username:"));          form.add(usernameField);
        form.add(new JLabel("Password:"));          form.add(passwordField);
        form.add(new JLabel("Confirm password:")); form.add(confirmField);

        JButton createButton = new JButton("Create Account");
        JButton cancelButton = new JButton("Cancel");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttons.add(createButton);
        buttons.add(cancelButton);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        createButton.addActionListener(e -> handleRegister());
        cancelButton.addActionListener(e -> dispose());
    }

    /** Runs when Create Account is clicked. */
    private void handleRegister() {
        String name     = nameField.getText().trim();
        String contact  = contactField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmField.getPassword());

        // ---- validation: stop at the first problem ----
        if (name.isEmpty() || contact.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all the fields.");
            return;
        }
        if (!contact.matches("\\d{10}")) {          // exactly 10 digits
            showError("Contact must be exactly 10 digits.");
            return;
        }
        if (username.length() < 3 || username.contains(" ")) {
            showError("Username must be at least 3 characters with no spaces.");
            return;
        }
        if (password.length() < 4) {
            showError("Password must be at least 4 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("The two passwords do not match.");
            return;
        }

        // ---- database work ----
        try {
            if (db.usernameExists(username)) {
                showError("That username is already taken. Please choose another.");
                return;
            }

            db.registerUser(name, contact, username, password);

            JOptionPane.showMessageDialog(this, "Account created! You can now log in.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save the account.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your details", JOptionPane.WARNING_MESSAGE);
    }
}
