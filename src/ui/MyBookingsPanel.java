package ui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import model.Customer;

/**
 * The "My Bookings" tab.
 * PLACEHOLDER: Phase 10 replaces this with a table of the customer's
 * bookings and a Cancel button.
 */
public class MyBookingsPanel extends JPanel {

    private final Customer customer;

    public MyBookingsPanel(Customer customer) {
        this.customer = customer;
        setLayout(new BorderLayout());
        add(new JLabel("Your bookings will appear here (Phase 10).", SwingConstants.CENTER),
                BorderLayout.CENTER);
    }

    public Customer getCustomer() {
        return customer;
    }
}