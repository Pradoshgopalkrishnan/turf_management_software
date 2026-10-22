package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import model.Customer;

/**
 * The "My Bookings" tab (Phase 10).
 *
 * Shows every booking of the logged-in customer, newest first, in a table:
 *     ID | Turf | Date | Slot | Total | Status
 * The Cancel Booking button is only enabled for a booking that can still be cancelled
 * (CONFIRMED and the slot has not started yet, see Booking.isCancellable()).
 */
public class MyBookingsPanel extends JPanel {

    private static final String RUPEE = "\u20B9";   // written as an escape so the file stays plain ASCII
    private static final Color GREEN = new Color(0, 128, 0);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);

    private final DatabaseManager db = new DatabaseManager();
    private final Customer customer;

    // Same order as the table rows: row 3 of the table is bookings.get(3)
    private List<Booking> bookings = new ArrayList<>();

    private DefaultTableModel bookingModel;
    private JTable bookingTable;
    private JButton cancelButton;
    private JLabel countLabel;

    public MyBookingsPanel(Customer customer) {
        this.customer = customer;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- top: "You have N bookings" ----
        countLabel = new JLabel(" ");
        add(countLabel, BorderLayout.NORTH);

        // ---- centre: the table (cells can't be edited) ----
        bookingModel = new DefaultTableModel(
                new String[] { "ID", "Turf", "Date", "Slot", "Total", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingTable = new JTable(bookingModel);
        bookingTable.setRowHeight(22);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.getColumnModel().getColumn(0).setPreferredWidth(40);   // ID needs little room
        bookingTable.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
        add(new JScrollPane(bookingTable), BorderLayout.CENTER);

        // ---- bottom: Cancel Booking button (off until a cancellable row is selected) ----
        cancelButton = new JButton("Cancel Booking");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelSelectedBooking());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.add(cancelButton);
        add(buttonRow, BorderLayout.SOUTH);

        // ---- listener added last, after every component exists ----
        bookingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCancelButton();
            }
        });

        loadBookings();
    }

    /**
     * Reloads this customer's bookings from MySQL into the table.
     * Public because the dashboard calls it whenever the customer opens this tab.
     */
    public void loadBookings() {
        bookingModel.setRowCount(0);                     // clear old rows
        bookings = new ArrayList<>();

        try {
            bookings = db.getBookingsByUser(customer.getUserId());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load your bookings.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }

        for (Booking b : bookings) {
            bookingModel.addRow(new Object[] {
                    b.getBookingId(),
                    b.getTurfName(),
                    DATE_FORMAT.format(b.getBookingDate()),
                    b.getTimeSlot(),
                    RUPEE + String.format("%.2f", b.getFinalTotal()),
                    b.getStatus()
            });
        }

        countLabel.setText(bookings.isEmpty()
                ? "You have no bookings yet."
                : "You have " + bookings.size() + " booking(s), newest first.");
        updateCancelButton();
    }

    /** The Cancel button is on only when the selected booking can still be cancelled. */
    private void updateCancelButton() {
        int row = bookingTable.getSelectedRow();         // -1 means nothing is selected
        boolean ok = row >= 0 && row < bookings.size() && bookings.get(row).isCancellable();
        cancelButton.setEnabled(ok);
    }

    /** Runs when Cancel Booking is clicked: re-check the rule, ask, then cancel. */
    private void cancelSelectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0 || row >= bookings.size()) {
            return;
        }
        Booking b = bookings.get(row);

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
                + b.getTurfName() + "\n"
                + DATE_FORMAT.format(b.getBookingDate()) + ", " + b.getTimeSlot() + "\n"
                + "Total " + RUPEE + String.format("%.2f", b.getFinalTotal());
        int answer = JOptionPane.showConfirmDialog(this, question, "Cancel booking",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean cancelled = db.cancelBooking(b.getBookingId(), customer.getUserId());
            if (cancelled) {
                JOptionPane.showMessageDialog(this,
                        "Booking cancelled. The slot is available again.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "This booking could not be cancelled.\nIt may already have been cancelled.",
                        "Not cancelled", JOptionPane.WARNING_MESSAGE);
            }
            loadBookings();                              // show the new status either way
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not cancel the booking.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Customer getCustomer() {
        return customer;
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