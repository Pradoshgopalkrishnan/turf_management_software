import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ui.LoginFrame;

/**
 * The program starts here. It just opens the login window.
 * Swing windows should be created on the "event thread", which is what
 * SwingUtilities.invokeLater does.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame().setVisible(true);
            } catch (RuntimeException e) {
                // Most likely cause: db.properties is missing
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
