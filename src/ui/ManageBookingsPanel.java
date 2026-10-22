package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import db.DatabaseManager;
import model.Booking;
import model.Turf;

/**
 * The "Bookings" tab of the admin dashboard (Phase 12).
 *
 * Shows EVERY customer's booking:   ID | Customer | Turf | Date | Slot | Total | Status
 * The two combo boxes at the top filter the list by date and / or turf ("All" = no filter).
 * The Cancel Booking button works on any customer's booking, as long as it can still be
 * cancelled (CONFIRMED and the slot has not started yet, see Booking.isCancellable()).
 *
 * The bookings are loaded from MySQL once (loadBookings) and the filters then work on that
 * list in memory (applyFilter), so changing a filter is instant.
 */
public class ManageBookingsPanel extends JPanel {

    private static final String RUPEE = "\u20B9";   // written as an escape so the file stays plain ASCII
    private static final Color GREEN = new Color(0, 128, 0);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);

    private final DatabaseManager db = new DatabaseManager();

    private List<Booking> allBookings = new ArrayList<>();     // everything from the database
    private List<Booking> shownBookings = new ArrayList<>();   // after the filters; same order as the table rows

    // The combo boxes show "All ..." first, then one entry per date / turf.
    // So combo item number i (for i >= 1) belongs to filterDates.get(i - 1) / filterTurfs.get(i - 1).
    private List<LocalDate> filterDates = new ArrayList<>();
    private List<Turf> filterTurfs = new ArrayList<>();

    // true while loadBookings() rebuilds the combo boxes, so that does not trigger applyFilter()
    private boolean loading = false;

    private JComboBox<String> dateCombo;
    private JComboBox<String> turfCombo;
    private DefaultTableModel bookingModel;
    private JTable bookingTable;
    private JButton cancelButton;
    private JLabel countLabel;

    public ManageBookingsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- top: the two filters ----
        dateCombo = new JComboBox<>();
        turfCombo = new JComboBox<>();

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.add(new JLabel("Date:"));
        filterRow.add(dateCombo);
        filterRow.add(new JLabel("Turf:"));
        filterRow.add(turfCombo);
        add(filterRow, BorderLayout.NORTH);

        // ---- centre: the table (cells can't be edited) ----
        bookingModel = new DefaultTableModel(
                new String[] { "ID", "Customer", "Turf", "Date", "Slot", "Total", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingTable = new JTable(bookingModel);
        bookingTable.setRowHeight(22);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.getColumnModel().getColumn(0).setPreferredWidth(40);   // ID needs little room
        bookingTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        add(new JScrollPane(bookingTable), BorderLayout.CENTER);

        // ---- bottom: "Showing X of Y" on the left, Cancel Booking on the right ----
        countLabel = new JLabel(" ");
        cancelButton = new JButton("Cancel Booking");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelSelectedBooking());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(countLabel, BorderLayout.WEST);
        bottom.add(cancelButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // ---- listeners added last, after every component exists ----
        dateCombo.addActionListener(e -> {
            if (!loading) {
                applyFilter();
            }
        });
        turfCombo.addActionListener(e -> {
            if (!loading) {
                applyFilter();
            }
        });
        bookingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCancelButton();
            }
        });

        loadBookings();
    }

    // =====================================================================
    // Loading and filtering
    // =====================================================================

    /**
     * Reloads all bookings and all turfs from MySQL, rebuilds the two filter combo boxes
     * (keeping the current choices where they still exist) and shows the filtered rows.
     * Public because the dashboard calls it whenever the admin opens this tab.
     */
    public void loadBookings() {
        LocalDate keepDate = getFilterDate();          // remember the current filters
        int keepTurfId = getFilterTurfId();

        allBookings = new ArrayList<>();
        List<Turf> turfs = new ArrayList<>();
        try {
            allBookings = db.getAllBookings();
            turfs = db.getAllTurfs();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load the bookings.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }

        loading = true;                                // rebuilding must not trigger applyFilter()
        rebuildDateCombo(keepDate);
        rebuildTurfCombo(turfs, keepTurfId);
        loading = false;

        applyFilter();
    }

    /** "All dates" plus one entry for every date that has at least one booking. */
    private void rebuildDateCombo(LocalDate keep) {
        filterDates = distinctDates(allBookings);

        dateCombo.removeAllItems();
        dateCombo.addItem("All dates");
        for (LocalDate d : filterDates) {
            dateCombo.addItem(DATE_FORMAT.format(d));
        }
        int index = filterDates.indexOf(keep);         // -1 if keep is null or no longer present
        dateCombo.setSelectedIndex(index + 1);         // -1 + 1 = 0 = "All dates"
    }

    /** "All turfs" plus one entry for every turf. */
    private void rebuildTurfCombo(List<Turf> turfs, int keepTurfId) {
        filterTurfs = turfs;

        turfCombo.removeAllItems();
        turfCombo.addItem("All turfs");
        int select = 0;
        for (int i = 0; i < turfs.size(); i++) {
            turfCombo.addItem(turfs.get(i).getName());
            if (turfs.get(i).getTurfId() == keepTurfId) {
                select = i + 1;
            }
        }
        turfCombo.setSelectedIndex(select);
    }

    /** The date chosen in the date combo, or null for "All dates". */
    private LocalDate getFilterDate() {
        int i = dateCombo.getSelectedIndex();
        return (i <= 0 || i > filterDates.size()) ? null : filterDates.get(i - 1);
    }

    /** The ID of the turf chosen in the turf combo, or 0 for "All turfs". */
    private int getFilterTurfId() {
        int i = turfCombo.getSelectedIndex();
        return (i <= 0 || i > filterTurfs.size()) ? 0 : filterTurfs.get(i - 1).getTurfId();
    }

    /** Applies the two filters to the loaded list and refills the table. */
    private void applyFilter() {
        shownBookings = filterBookings(allBookings, getFilterDate(), getFilterTurfId());

        bookingModel.setRowCount(0);                   // clear old rows
        for (Booking b : shownBookings) {
            bookingModel.addRow(new Object[] {
                    b.getBookingId(),
                    b.getCustomerName(),
                    b.getTurfName(),
                    DATE_FORMAT.format(b.getBookingDate()),
                    b.getTimeSlot(),
                    RUPEE + String.format("%.2f", b.getFinalTotal()),
                    b.getStatus()
            });
        }

        countLabel.setText(allBookings.isEmpty()
                ? "There are no bookings yet."
                : "Showing " + shownBookings.size() + " of " + allBookings.size() + " booking(s).");
        updateCancelButton();
    }

    /**
     * Keeps only the bookings that match the filters.
     * date == null means "any date", turfId == 0 means "any turf".
     * (Static and free of any Swing code, so it can be tested on its own.)
     */
    static List<Booking> filterBookings(List<Booking> all, LocalDate date, int turfId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : all) {
            if (date != null && !b.getBookingDate().equals(date)) {
                continue;
            }
            if (turfId != 0 && b.getTurfId() != turfId) {
                continue;
            }
            result.add(b);
        }
        return result;
    }

    /** Every different booking date in the list, latest first, each only once. */
    static List<LocalDate> distinctDates(List<Booking> all) {
        TreeSet<LocalDate> dates = new TreeSet<>();    // a TreeSet is sorted and drops duplicates
        for (Booking b : all) {
            dates.add(b.getBookingDate());
        }
        return new ArrayList<>(dates.descendingSet());
    }

    // =====================================================================
    // Cancel
    // =====================================================================

    /** The Cancel button is on only when the selected booking can still be cancelled. */
    private void updateCancelButton() {
        int row = bookingTable.getSelectedRow();       // -1 means nothing is selected
        boolean ok = row >= 0 && row < shownBookings.size() && shownBookings.get(row).isCancellable();
        cancelButton.setEnabled(ok);
    }

    /** Runs when Cancel Booking is clicked: re-check the rule, ask, then cancel. */
    private void cancelSelectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0 || row >= shownBookings.size()) {
            return;
        }
        Booking b = shownBookings.get(row);

        // the slot may have started while this tab was open, so check the rule again
        if (!b.isCancellable()) {
            JOptionPane.showMessageDialog(this,
                    "This booking can no longer be cancelled.\n"
                  + "Only confirmed bookings whose slot has not started yet can be cancelled.",
                    "Cannot cancel", JOptionPane.WARNING_MESSAGE);
            loadBookings();
            return;
        }

        String question = "Cancel this booking?\n"
                + "Customer: " + b.getCustomerName() + "\n"
                + b.getTurfName() + "\n"
                + DATE_FORMAT.format(b.getBookingDate()) + ", " + b.getTimeSlot() + "\n"
                + "Total " + RUPEE + String.format("%.2f", b.getFinalTotal());
        int answer = JOptionPane.showConfirmDialog(this, question, "Cancel booking",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean cancelled = db.cancelBookingAsAdmin(b.getBookingId());
            if (cancelled) {
                JOptionPane.showMessageDialog(this,
                        "Booking cancelled. The slot is available again.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "This booking could not be cancelled.\nIt may already have been cancelled.",
                        "Not cancelled", JOptionPane.WARNING_MESSAGE);
            }
            loadBookings();                            // show the new status either way
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not cancel the booking.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Status column: CONFIRMED in green, CANCELLED in grey. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                // set the colour in BOTH cases (the renderer remembers the last colour it was given)
                c.setForeground("CANCELLED".equals(value) ? Color.GRAY : GREEN);
            }
            return c;
        }
    }
}
