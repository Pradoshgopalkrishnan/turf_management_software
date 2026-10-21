package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import db.DatabaseManager;
import model.Customer;
import model.Turf;

/**
 * The "Book a Slot" tab.
 *
 * Phase 5 : pick a turf from a combo box and see its details.
 * Phase 6 : pick a date (next 14 days) and see which hourly slots are
 *           Available or Booked for that turf and date.
 * Phase 7-9: price panel, promo code and Confirm button come after that.
 */
public class BookSlotPanel extends JPanel {

    private static final String RUPEE = "\u20B9";   // written as an escape so the file stays plain ASCII

    // Slots run 06:00 to 23:00, one hour each (06-07 ... 22-23) = 17 slots a day
    private static final int FIRST_HOUR = 6;
    private static final int LAST_HOUR  = 23;
    private static final int DAYS_AHEAD = 14;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);

    private final DatabaseManager db = new DatabaseManager();
    private final Customer customer;

    private JComboBox<Turf> turfCombo;
    private JLabel locationValue;
    private JLabel capacityValue;
    private JLabel priceValue;

    private JComboBox<LocalDate> dateCombo;
    private DefaultTableModel slotModel;
    private JTable slotTable;

    public BookSlotPanel(Customer customer) {
        this.customer = customer;
        setLayout(new BorderLayout(0, 15));
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

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.add(selectRow, BorderLayout.NORTH);
        top.add(details, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- Phase 6: date row + slot table ----
        add(buildSlotArea(), BorderLayout.CENTER);

        // ---- listeners are added LAST, after every component above exists,
        //      because changing a selection calls refreshSlots() ----
        turfCombo.addActionListener(e -> {
            showSelectedTurf();
            refreshSlots();
        });
        dateCombo.addActionListener(e -> refreshSlots());

        loadTurfs();
    }

    /** Builds the date combo box and the Slot | Status table. */
    private JPanel buildSlotArea() {
        // date combo: today + the next 13 days, so past dates can never be chosen
        dateCombo = new JComboBox<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < DAYS_AHEAD; i++) {
            dateCombo.addItem(today.plusDays(i));
        }
        // the combo stores LocalDate objects but shows them as "Tue, 20 Oct 2026"
        dateCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LocalDate) {
                    setText(DATE_FORMAT.format((LocalDate) value));
                }
                return this;
            }
        });

        JPanel dateRow = new JPanel(new BorderLayout(10, 0));
        dateRow.add(new JLabel("Select date:"), BorderLayout.WEST);
        dateRow.add(dateCombo, BorderLayout.CENTER);

        // table: two columns, cells can't be edited by the user
        slotModel = new DefaultTableModel(new String[] { "Slot", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        slotTable = new JTable(slotModel);
        slotTable.setRowHeight(22);
        slotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        slotTable.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());

        JPanel area = new JPanel(new BorderLayout(0, 10));
        area.add(dateRow, BorderLayout.NORTH);
        area.add(new JScrollPane(slotTable), BorderLayout.CENTER);
        return area;
    }

    /** Fills the combo box with every turf from the database. */
    private void loadTurfs() {
        try {
            List<Turf> turfs = db.getAllTurfs();
            for (Turf t : turfs) {
                // The first addItem() selects that turf automatically, which fires the
                // turf listener, which shows its details and loads the slot table.
                turfCombo.addItem(t);        // the combo shows Turf.toString() = the name
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load turfs from the database.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }

        if (turfCombo.getItemCount() == 0) {
            turfCombo.setEnabled(false);
            dateCombo.setEnabled(false);
            locationValue.setText("No turfs available");
            capacityValue.setText("-");
            priceValue.setText("-");
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

    /**
     * Reloads the slot table for the selected turf + date:
     * loops through every slot, asks the database if it is free, and
     * writes "Available" or "Booked".
     */
    private void refreshSlots() {
        slotModel.setRowCount(0);                       // clear old rows

        Turf turf = getSelectedTurf();
        LocalDate date = getSelectedDate();
        if (turf == null || date == null) {
            return;
        }
        java.sql.Date sqlDate = java.sql.Date.valueOf(date);

        for (int hour = FIRST_HOUR; hour < LAST_HOUR; hour++) {
            String slot = String.format("%02d:00-%02d:00", hour, hour + 1);
            try {
                boolean free = db.checkSlot(turf.getTurfId(), sqlDate, slot);
                slotModel.addRow(new Object[] { slot, free ? "Available" : "Booked" });
            } catch (SQLException ex) {
                // one dialog only, then stop (otherwise every slot would pop up an error)
                slotModel.setRowCount(0);
                JOptionPane.showMessageDialog(this,
                        "Could not check slot availability.\n" + ex.getMessage(),
                        "Database error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    /** Used by later phases (slots, price, booking) to know which turf is chosen. */
    public Turf getSelectedTurf() {
        return (Turf) turfCombo.getSelectedItem();
    }

    /** The date chosen in the date combo box (null if none). */
    public LocalDate getSelectedDate() {
        return (LocalDate) dateCombo.getSelectedItem();
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

    /** Draws "Booked" in red and "Available" in green. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        private static final Color GREEN = new Color(0, 128, 0);   // dark green, readable on white

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                // Set the colour explicitly in BOTH cases. DefaultTableCellRenderer
                // remembers whatever colour was last set, so without the else-part
                // red would stick to every cell drawn after a "Booked" one.
                c.setForeground("Booked".equals(value) ? Color.RED : GREEN);
            }
            return c;
        }
    }
}