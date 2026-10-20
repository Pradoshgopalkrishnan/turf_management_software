package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import db.DatabaseManager;
import model.Customer;
import model.Turf;

/**
 * The "Book a Slot" tab.
 *
 * Phase 5 : pick a turf from a combo box and see its details.
 * Phase 6 : date list + slot table will be added below the turf details.
 * Phase 7-9: price panel, promo code and Confirm button come after that.
 */
public class BookSlotPanel extends JPanel {

    private static final String RUPEE = "\u20B9";   // written as an escape so the file stays plain ASCII

    private final DatabaseManager db = new DatabaseManager();
    private final Customer customer;

    private JComboBox<Turf> turfCombo;
    private JLabel locationValue;
    private JLabel capacityValue;
    private JLabel priceValue;

    public BookSlotPanel(Customer customer) {
        this.customer = customer;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- row 1: "Select turf" + combo box ----
        turfCombo = new JComboBox<>();
        JPanel selectRow = new JPanel(new BorderLayout(10, 0));
        selectRow.add(new JLabel("Select turf:"), BorderLayout.WEST);
        selectRow.add(turfCombo, BorderLayout.CENTER);

        // ---- turf details (three label pairs) ----
        locationValue = valueLabel();
        capacityValue = valueLabel();
        priceValue    = valueLabel();

        JPanel details = new JPanel(new GridLayout(3, 2, 10, 8));
        details.setBorder(BorderFactory.createTitledBorder("Turf details"));
        details.add(new JLabel("Location:"));
        details.add(locationValue);
        details.add(new JLabel("Capacity (players):"));
        details.add(capacityValue);
        details.add(new JLabel("Base price per hour:"));
        details.add(priceValue);

        // ---- stack the two rows at the top; Phase 6 fills the space below ----
        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.add(selectRow, BorderLayout.NORTH);
        top.add(details, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- when the selection changes, refresh the details ----
        turfCombo.addActionListener(e -> showSelectedTurf());

        loadTurfs();
    }

    /** Fills the combo box with every turf from the database. */
    private void loadTurfs() {
        try {
            List<Turf> turfs = db.getAllTurfs();
            for (Turf t : turfs) {
                turfCombo.addItem(t);        // the combo shows Turf.toString() = the name
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load turfs from the database.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }

        if (turfCombo.getItemCount() == 0) {
            turfCombo.setEnabled(false);
            locationValue.setText("No turfs available");
            capacityValue.setText("-");
            priceValue.setText("-");
        } else {
            turfCombo.setSelectedIndex(0);
            showSelectedTurf();   // addItem() already selects the first item, this just makes it explicit
        }
    }

    /** Shows the details of whichever turf is selected. */
    private void showSelectedTurf() {
        Turf t = getSelectedTurf();
        if (t == null) {
            return;
        }
        locationValue.setText(t.getLocation() == null ? "-" : t.getLocation());
        capacityValue.setText(String.valueOf(t.getCapacity()));
        priceValue.setText(RUPEE + String.format("%.2f", t.getBasePrice()));
    }

    /** Used by later phases (slots, price, booking) to know which turf is chosen. */
    public Turf getSelectedTurf() {
        return (Turf) turfCombo.getSelectedItem();
    }

    /** The logged-in customer (needed in Phase 9 when saving a booking). */
    public Customer getCustomer() {
        return customer;
    }

    private JLabel valueLabel() {
        JLabel l = new JLabel("-", SwingConstants.LEFT);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }
}