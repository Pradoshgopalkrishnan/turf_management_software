package model;

import java.time.LocalDate;

/**
 * A discount code. A code is usable only if it is active AND not expired.
 */
public class PromoCode {

    private String code;
    private double discountPercent;
    private LocalDate expiryDate;
    private boolean active;

    public PromoCode(String code, double discountPercent, LocalDate expiryDate, boolean active) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.expiryDate = expiryDate;
        this.active = active;
    }

    /** True if the admin has not switched it off and today is on or before the expiry date. */
    public boolean isValid() {
        boolean notExpired = !expiryDate.isBefore(LocalDate.now());
        return active && notExpired;
    }

    /** The discount as a percentage, e.g. 10 means 10% off. */
    public double getDiscount() {
        return discountPercent;
    }

    public String getCode()          { return code; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public boolean isActive()        { return active; }

    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }
    public void setExpiryDate(LocalDate expiryDate)        { this.expiryDate = expiryDate; }
    public void setActive(boolean active)                  { this.active = active; }
}