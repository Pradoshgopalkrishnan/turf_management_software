package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import db.DatabaseManager;
import model.PromoCode;

/**
 * The "Promo Codes" tab of the admin dashboard (Phase 13).
 *
 *   top    : table of every promo code  (Code | Discount | Expires | Active | Status)
 *   bottom : fields (code, discount %, expiry date, active tick box) + Add / Update / Delete / Clear
 *
 * Clicking a row fills the fields. The code itself is the table's key, so it can only be
 * typed when ADDING a code; for an existing code only the discount, expiry date and the
 * active tick box can be changed.
 */
public class ManagePromoPanel extends JPanel {

    private static final Color GREEN = new Color(0, 128, 0);

    private final DatabaseManager db = new DatabaseManager();

    // Same order as the table rows: row 2 of the table is promos.get(2)
    private List<PromoCode> promos = new ArrayList<>();

    private DefaultTableModel promoModel;
    private JTable promoTable;

    private JTextField codeField;
    private JTextField discountField;
    private JTextField expiryField;
    private JCheckBox activeBox;

    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;

    private JLabel statusLabel;

    public ManagePromoPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- centre: the table (cells can't be edited) ----
        promoModel = new DefaultTableModel(
                new String[] { "Code", "Discount", "Expires", "Active", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        promoTable = new JTable(promoModel);
        promoTable.setRowHeight(22);
        promoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(promoTable), BorderLayout.CENTER);

        // ---- bottom: form, buttons and a status line ----
        codeField     = new JTextField();
        discountField = new JTextField();
        expiryField   = new JTextField();
        activeBox     = new JCheckBox("Active (customers can use this code)", true);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Promo code details"));
        form.add(new JLabel("Code:"));
        form.add(codeField);
        form.add(new JLabel("Discount (1 to 100 %):"));
        form.add(discountField);
        form.add(new JLabel("Expiry date (yyyy-MM-dd):"));
        form.add(expiryField);
        form.add(new JLabel("Active:"));
        form.add(activeBox);

        addButton    = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        clearButton  = new JButton("Clear");
        updateButton.setEnabled(false);                  // these two need a selected row
        deleteButton.setEnabled(false);

        addButton.addActionListener(e -> addPromo());
        updateButton.addActionListener(e -> updatePromo());
        deleteButton.addActionListener(e -> deletePromo());
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
        promoTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRowSelected();
            }
        });

        loadPromos();
    }

    // =====================================================================
    // Loading and selecting
    // =====================================================================

    /** Reloads every promo code from MySQL into the table. */
    private void loadPromos() {
        promoModel.setRowCount(0);                       // clear old rows
        promos = new ArrayList<>();

        try {
            promos = db.getAllPromoCodes();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load the promo codes.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }

        for (PromoCode p : promos) {
            promoModel.addRow(new Object[] {
                    p.getCode(),
                    percent(p.getDiscount()) + "%",
                    p.getExpiryDate().toString(),        // yyyy-MM-dd, the same format the admin types
                    p.isActive() ? "Yes" : "No",
                    statusOf(p)
            });
        }
    }

    /** What a customer would get today: Valid, Inactive (switched off) or Expired (date passed). */
    private static String statusOf(PromoCode p) {
        if (p.isValid()) {
            return "Valid";
        }
        return p.isActive() ? "Expired" : "Inactive";
    }

    /**
     * A row was clicked: copy that code into the fields. While a row is selected the code
     * cannot be edited (it is the key) and Add is off; Update and Delete are on.
     */
    private void onRowSelected() {
        int row = promoTable.getSelectedRow();           // -1 means nothing is selected
        boolean hasRow = row >= 0 && row < promos.size();

        if (hasRow) {
            PromoCode p = promos.get(row);
            codeField.setText(p.getCode());
            discountField.setText(percent(p.getDiscount()));
            expiryField.setText(p.getExpiryDate().toString());
            activeBox.setSelected(p.isActive());
            statusLabel.setText(" ");
        }
        codeField.setEditable(!hasRow);
        addButton.setEnabled(!hasRow);
        updateButton.setEnabled(hasRow);
        deleteButton.setEnabled(hasRow);
    }

    // =====================================================================
    // The four buttons
    // =====================================================================

    private void addPromo() {
        PromoCode p = readForm();
        if (p == null) {
            return;                                      // the error is already shown
        }
        try {
            if (db.addPromoCode(p)) {
                clearForm();
                loadPromos();
                showOk("Promo code " + p.getCode() + " added.");
            } else {
                showError("A promo code named " + p.getCode() + " already exists.");
            }
        } catch (SQLException ex) {
            showDbError("Could not add the promo code.", ex);
        }
    }

    private void updatePromo() {
        int row = promoTable.getSelectedRow();
        if (row < 0 || row >= promos.size()) {
            return;
        }
        PromoCode p = readForm();
        if (p == null) {
            return;
        }
        try {
            boolean updated = db.updatePromoCode(p);
            clearForm();
            loadPromos();
            if (updated) {
                showOk("Promo code " + p.getCode() + " updated.");
            } else {
                showError("That promo code no longer exists. The list has been refreshed.");
            }
        } catch (SQLException ex) {
            showDbError("Could not update the promo code.", ex);
        }
    }

    private void deletePromo() {
        int row = promoTable.getSelectedRow();
        if (row < 0 || row >= promos.size()) {
            return;
        }
        PromoCode p = promos.get(row);

        int answer = JOptionPane.showConfirmDialog(this,
                "Delete promo code " + p.getCode() + "?\n"
              + "Bookings that already used it keep their record.",
                "Delete promo code", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted = db.deletePromoCode(p.getCode());
            clearForm();
            loadPromos();
            if (deleted) {
                showOk("Promo code " + p.getCode() + " deleted.");
            } else {
                showError("That promo code no longer exists. The list has been refreshed.");
            }
        } catch (SQLException ex) {
            showDbError("Could not delete the promo code.", ex);
        }
    }

    /** Empties the fields (active ticked again) and un-selects the table row. */
    private void clearForm() {
        promoTable.clearSelection();                     // also switches Add / Update / Delete back
        codeField.setText("");
        discountField.setText("");
        expiryField.setText("");
        activeBox.setSelected(true);
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
    static String validate(String code, String discount, String expiry) {
        code = code.trim();
        discount = discount.trim();
        expiry = expiry.trim();

        if (code.isEmpty()) {
            return "Code cannot be empty.";
        }
        if (code.length() > 20) {
            return "Code is too long (20 characters at most).";
        }
        if (!code.matches("[A-Za-z0-9_-]+")) {
            return "Code may only contain letters, digits, - and _ (no spaces).";
        }

        double d;
        try {
            d = Double.parseDouble(discount);
        } catch (NumberFormatException e) {
            return "Discount must be a number.";
        }
        if (Double.isNaN(d) || d < 1 || d > 100) {
            return "Discount must be between 1 and 100.";
        }

        // first the shape (so "+12345-01-01" or "2026-1-5" are refused), then LocalDate
        // checks that it is a real date (2026-02-30 is not)
        String dateError = "Expiry date must be a real date written as yyyy-MM-dd, e.g. 2026-12-31.";
        if (!expiry.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dateError;
        }
        try {
            LocalDate.parse(expiry);
        } catch (DateTimeParseException e) {
            return dateError;
        }
        return null;
    }

    /** Validates the form and builds a PromoCode from it. Returns null (and shows why) if invalid. */
    private PromoCode readForm() {
        String error = validate(codeField.getText(), discountField.getText(), expiryField.getText());
        if (error != null) {
            showError(error);
            return null;
        }
        return new PromoCode(
                codeField.getText().trim().toUpperCase(Locale.ROOT),   // codes are stored in capitals
                Double.parseDouble(discountField.getText().trim()),
                LocalDate.parse(expiryField.getText().trim()),
                activeBox.isSelected());
    }

    /** 10.0 -> "10", 12.5 -> "12.5" (so a whole percentage has no ".0"). */
    private static String percent(double p) {
        if (p == Math.rint(p)) {
            return String.valueOf((int) p);
        }
        return String.valueOf(p);
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
