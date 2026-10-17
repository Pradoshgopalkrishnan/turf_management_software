package model;

import java.time.LocalDate;

/**
 * Throwaway test for Phase 3. No database, no GUI, just Java.
 * Run it and compare the output with the expected values.
 */
public class ModelTest {

    public static void main(String[] args) {

        // ---------- Test 1: polymorphism ----------
        System.out.println("=== Polymorphism test ===");
        User[] users = {
            new Customer(2, "Test Customer", "9111111111", "cust1", "pass123"),
            new Admin(1, "Admin Owner", "9000000000", "admin", "admin123")
        };
        for (User u : users) {
            System.out.println("Role: " + u.getRole());
            u.openDashboard();     // same call, different behaviour for each type
        }

        // ---------- Test 2: price calculation ----------
        System.out.println("\n=== Price tests (base Rs.1000, weekend 1.2, evening 1.3) ===");
        double base = 1000, weekendMult = 1.2, eveningMult = 1.3;

        LocalDate monday   = LocalDate.of(2026, 10, 19);
        LocalDate saturday = LocalDate.of(2026, 10, 17);

        priceTest("Weekday day     ", monday,   "10:00-11:00", base, weekendMult, eveningMult, 0);
        priceTest("Weekday evening ", monday,   "19:00-20:00", base, weekendMult, eveningMult, 0);
        priceTest("Weekend day     ", saturday, "10:00-11:00", base, weekendMult, eveningMult, 0);
        priceTest("Weekend evening ", saturday, "19:00-20:00", base, weekendMult, eveningMult, 0);
        priceTest("Weekend eve + 10%", saturday, "19:00-20:00", base, weekendMult, eveningMult, 10);

        // ---------- Test 3: promo validity ----------
        System.out.println("\n=== Promo tests ===");
        PromoCode good     = new PromoCode("WELCOME10", 10, LocalDate.of(2026, 12, 31), true);
        PromoCode expired  = new PromoCode("OLD20",     20, LocalDate.of(2026, 1, 1),   true);
        PromoCode inactive = new PromoCode("OFF50",     50, LocalDate.of(2026, 12, 31), false);

        System.out.println("WELCOME10 valid? " + good.isValid());       // expect true
        System.out.println("OLD20 valid?     " + expired.isValid());    // expect false
        System.out.println("OFF50 valid?     " + inactive.isValid());   // expect false
    }

    private static void priceTest(String label, LocalDate date, String slot, double base,
                                  double weekendMult, double eveningMult, double discountPercent) {
        Booking b = new Booking(2, 1, date, slot, base);
        b.setSurgeMultiplier(Booking.determineSurge(date, slot, weekendMult, eveningMult));
        b.calculateFinalTotal(discountPercent);
        System.out.printf("%s -> surge x%.1f, total Rs.%.2f%n",
                label, b.getSurgeMultiplier(), b.getFinalTotal());
    }
}