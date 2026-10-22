package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import db.DatabaseManager;
import model.Turf;

/**
 * The "Turfs" tab of the admin dashboard (Phase 11).
 *
 *   top    : table of every turf
 *   bottom : text fields (name, location, capacity, base price) + Add / Update / Delete / Clear
 *
 * Clicking a row fills the text fields, so the admin can edit that turf.
 * A turf that has bookings cannot be deleted.
 */
public class ManageTurfsPanel extends JPanel {

    private static final String RUPEE = "\u20B9";   // written as an escape so the file stays plain ASCII
    private static final Color GREEN = new Color(0, 128, 0);

    private final DatabaseManager db = new DatabaseManager();

    // Same order as the table rows: row 2 of the table is turfs.get(2)
    private List<Turf> turfs = new ArrayList<>();

    private DefaultTableModel turfModel;
    private JTable turfTable;

    private JTextField nameField;
    private JTextField locationField;
    private JTextField capacityField;
    private JTextField priceField;

    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;

    private JLabel statusLabel;

    public ManageTurfsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- centre: the table (cells can't be edited) ----
        turfModel = new DefaultTableModel(
                new String[] { "ID", "Name", "Location", "Capacity", "Base price" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        turfTable = new JTable(turfModel);
        turfTable.setRowHeight(22);
        turfTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        turfTable.getColumnModel().getColumn(0).setPreferredWidth(40);   // ID needs little room
        add(new JScrollPane(turfTable), BorderLayout.CENTER);

        // ---- bottom: form, buttons and a status line ----
        nameField     = new JTextField();
        locationField = new JTextField();
        capacityField = new JTextField();
        priceField    = new JTextField();

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Turf details"));
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Location (optional):"));
        form.add(locationField);
        form.add(new JLabel("Capacity (players):"));
        form.add(capacityField);
        form.add(new JLabel("Base price per hour (" + RUPEE + "):"));
        form.add(priceField);

        addButton    = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        clearButton  = new JButton("Clear");
        updateButton.setEnabled(false);                  // these two need a selected row
        deleteButton.setEnabled(false);

        addButton.addActionListener(e -> addTurf());
        updateButton.addActionListener(e -> updateTurf());
        deleteButton.addActionListener(e -> deleteTurf());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.add(addButton);
        buttonRow.add(updateButton);
        buttonRow.add(deleteButton);
        buttonRow.add(clearButton);

        statusLabel = new JLabel(" ");

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.add(form, BorderLayout.NORTH);
        bottom.add(buttonRow, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // ---- listener added last, after every component exists ----
        turfTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRowSelected();
            }
        });

        loadTurfs();
    }

    // =====================================================================
    // Loading and selecting
    // =====================================================================

    /** Reloads every turf from MySQL into the table. */
    private void loadTurfs() {
        turfModel.setRowCount(0);                        // clear old rows
        turfs = new ArrayList<>();

        try {
            turfs = db.getAllTurfs();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load the turfs.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }

        for (Turf t : turfs) {
            turfModel.addRow(new Object[] {
                    t.getTurfId(),
                    t.getName(),
                    t.getLocation() == null ? "-" : t.getLocation(),
                    t.getCapacity(),
                    RUPEE + String.format("%.2f", t.getBasePrice())
            });
        }
    }

    /** A row was clicked: copy that turf into the text fields and enable Update / Delete. */
    private void onRowSelected() {
        int row = turfTable.getSelectedRow();            // -1 means nothing is selected
        boolean hasRow = row >= 0 && row < turfs.size();

        if (hasRow) {
            Turf t = turfs.get(row);
            nameField.setText(t.getName());
            locationField.setText(t.getLocation() == null ? "" : t.getLocation());
            capacityField.setText(String.valueOf(t.getCapacity()));
            priceField.setText(String.valueOf(t.getBasePrice()));
            statusLabel.setText(" ");
        }
        updateButton.setEnabled(hasRow);
        deleteButton.setEnabled(hasRow);
    }

    // =====================================================================
    // The four buttons
    // =====================================================================

    private void addTurf() {
        Turf t = readForm(0);                            // 0 = no ID yet, MySQL creates it
        if (t == null) {
            return;                                      // the error is already shown
        }
        try {
            db.addTurf(t.getName(), t.getLocation(), t.getCapacity(), t.getBasePrice());
            clearForm();
            loadTurfs();
            showOk("Turf \"" + t.getName() + "\" added.");
        } catch (SQLException ex) {
            showDbError("Could not add the turf.", ex);
        }
    }

    private void updateTurf() {
        int row = turfTable.getSelectedRow();
        if (row < 0 || row >= turfs.size()) {
            return;
        }
        Turf t = readForm(turfs.get(row).getTurfId());
        if (t == null) {
            return;
        }
        try {
            boolean updated = db.updateTurf(t);
            clearForm();
            loadTurfs();
            if (updated) {
                showOk("Turf \"" + t.getName() + "\" updated.");
            } else {
                showError("That turf no longer exists. The list has been refreshed.");
            }
        } catch (SQLException ex) {
            showDbError("Could not update the turf.", ex);
        }
    }

    private void deleteTurf() {
        int row = turfTable.getSelectedRow();
        if (row < 0 || row >= turfs.size()) {
            return;
        }
        Turf t = turfs.get(row);

        try {
            // the rule: a turf that has bookings can never be deleted
            int bookingCount = db.countBookingsForTurf(t.getTurfId());
            if (bookingCount > 0) {
                JOptionPane.showMessageDialog(this,
                        "\"" + t.getName() + "\" cannot be deleted.\n"
                      + "It has " + bookingCount + " booking(s) (cancelled ones count too).\n"
                      + "Turfs with bookings are kept so the booking history stays complete.",
                        "Cannot delete", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int answer = JOptionPane.showConfirmDialog(this,
                    "Delete \"" + t.getName() + "\"?\nThis cannot be undone.",
                    "Delete turf", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }

            boolean deleted = db.deleteTurf(t.getTurfId());
            clearForm();
            loadTurfs();
            if (deleted) {
                showOk("Turf \"" + t.getName() + "\" deleted.");
            } else {
                showError("That turf no longer exists. The list has been refreshed.");
            }
        } catch (SQLException ex) {
            showDbError("Could not delete the turf.", ex);
        }
    }

    /** Empties the text fields and un-selects the table row. */
    private void clearForm() {
        turfTable.clearSelection();                      // also switches Update / Delete off
        nameField.setText("");
        locationField.setText("");
        capacityField.setText("");
        priceField.setText("");
        statusLabel.setText(" ");
    }

    // =====================================================================
    // Validation
    // =====================================================================

    /**
     * Checks the text typed in the form. Returns an error message,
     * or null when everything is fine.
     * (Static and free of any Swing code, so it can be tested on its own.)
     */
    static String validate(String name, String location, String capacity, String price) {
        name = name.trim();
        location = location.trim();
        capacity = capacity.trim();
        price = price.trim();

        if (name.isEmpty()) {
            return "Name cannot be empty.";
        }
        if (name.length() > 100) {
            return "Name is too long (100 characters at most).";
        }
        if (location.length() > 150) {
            return "Location is too long (150 characters at most).";
        }

        int cap;
        try {
            cap = Integer.parseInt(capacity);
        } catch (NumberFormatException e) {
            return "Capacity must be a whole number.";
        }
        if (cap <= 0) {
            return "Capacity must be greater than 0.";
        }

        double p;
        try {
            p = Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return "Base price must be a number.";
        }
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0) {
            return "Base price must be greater than 0.";
        }
        return null;
    }

    /** Validates the form and builds a Turf from it. Returns null (and shows why) if it is invalid. */
    private Turf readForm(int turfId) {
        String error = validate(nameField.getText(), locationField.getText(),
                capacityField.getText(), priceField.getText());
        if (error != null) {
            showError(error);
            return null;
        }
        double price = Double.parseDouble(priceField.getText().trim());
        price = Math.round(price * 100) / 100.0;         // keep two decimals, like money
        return new Turf(turfId,
                nameField.getText().trim(),
                locationField.getText().trim(),
                Integer.parseInt(capacityField.getText().trim()),
                price);
    }

    // =====================================================================
    // Messages
    // =====================================================================

    private void showOk(String message) {
        statusLabel.setForeground(GREEN);
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }

    private void showDbError(String what, SQLException ex) {
        JOptionPane.showMessageDialog(this, what + "\n" + ex.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
