package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import db.DatabaseManager;

/**
 * The "Pricing" tab of the admin dashboard (Phase 13).
 *
 * Two text fields (weekend multiplier, evening multiplier) and a Save button.
 * Customers see the new prices straight away, because the Book tab reads the
 * multipliers from MySQL every time a slot is clicked.
 */
public class ManagePricingPanel extends JPanel {

    // The surge never lowers a price (Booking.determineSurge starts from 1.0 and takes the
    // higher multiplier), so 1.0 is the lowest useful value. 5.0 is a typo guard: typing 13
    // instead of 1.3 would charge 13 times the price.
    private static final double MIN_MULTIPLIER = 1.0;
    private static final double MAX_MULTIPLIER = 5.0;

    private static final Color GREEN = new Color(0, 128, 0);

    private final DatabaseManager db = new DatabaseManager();

    private JTextField weekendField;
    private JTextField eveningField;
    private JLabel statusLabel;

    public ManagePricingPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- a short explanation ----
        JPanel info = new JPanel(new GridLayout(3, 1, 0, 2));
        info.add(new JLabel("A multiplier raises the base price of a slot: price = base price x multiplier."));
        info.add(new JLabel("1.0 means no surge. If a slot is both weekend and evening, the HIGHER multiplier is used."));
        info.add(new JLabel("Existing bookings keep the price they were made with."));

        // ---- the two fields ----
        weekendField = new JTextField(10);
        eveningField = new JTextField(10);

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Surge multipliers"));
        form.add(new JLabel("Weekend multiplier (Saturday and Sunday):"));
        form.add(weekendField);
        form.add(new JLabel("Evening multiplier (slots starting at 18:00 or later):"));
        form.add(eveningField);

        // ---- Save button and status line ----
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> save());
        weekendField.addActionListener(e -> save());     // Enter in a field = Save
        eveningField.addActionListener(e -> save());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.add(saveButton);

        statusLabel = new JLabel(" ");

        JPanel actions = new JPanel(new BorderLayout(0, 8));
        actions.add(buttonRow, BorderLayout.NORTH);
        actions.add(statusLabel, BorderLayout.CENTER);

        // stacked at the top, the rest of the tab stays empty
        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.add(info, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        loadValues();
    }

    /** Shows the multipliers that are saved in MySQL right now. */
    private void loadValues() {
        try {
            weekendField.setText(format(db.getMultiplier("WEEKEND")));
            eveningField.setText(format(db.getMultiplier("EVENING")));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load the multipliers.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Validates both fields, then saves both (all or nothing). */
    private void save() {
        String error = validateMultiplier("Weekend multiplier", weekendField.getText());
        if (error == null) {
            error = validateMultiplier("Evening multiplier", eveningField.getText());
        }
        if (error != null) {
            showError(error);
            return;
        }

        double weekend = round2(Double.parseDouble(weekendField.getText().trim()));
        double evening = round2(Double.parseDouble(eveningField.getText().trim()));

        try {
            db.saveMultipliers(weekend, evening);
            weekendField.setText(format(weekend));       // show exactly what was saved
            eveningField.setText(format(evening));
            showOk("Saved: weekend x" + format(weekend) + ", evening x" + format(evening)
                    + ". Customers see the new prices straight away.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save the multipliers.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Checks one multiplier typed in a field. Returns an error message, or null if it is fine.
     * (Static and free of any Swing code, so it can be tested on its own.)
     */
    static String validateMultiplier(String name, String text) {
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return name + " must be a number, for example 1.30.";
        }
        if (Double.isNaN(value) || value < MIN_MULTIPLIER || value > MAX_MULTIPLIER) {
            return name + " must be between " + format(MIN_MULTIPLIER) + " and "
                    + format(MAX_MULTIPLIER) + " (1.00 means no surge).";
        }
        return null;
    }

    /** Two decimals with a dot, whatever the computer's language is (so it can be typed back and parsed). */
    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private void showOk(String message) {
        statusLabel.setForeground(GREEN);
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }
}
