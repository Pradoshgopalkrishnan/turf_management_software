package model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * One booking: who booked which turf, on what date and slot, and the price.
 *
 * Price formula:  finalTotal = (basePrice x surgeMultiplier) - discountAmount
 */
public class Booking {

    private int bookingId;            // set by MySQL after saving
    private int userId;
    private int turfId;
    private LocalDate bookingDate;
    private String timeSlot;          // e.g. "18:00-19:00"
    private double basePrice;
    private double surgeMultiplier = 1.0;
    private String promoCode;         // null if no promo used
    private double discountAmount = 0;
    private double finalTotal;
    private String status = "CONFIRMED";

    public Booking(int userId, int turfId, LocalDate bookingDate, String timeSlot, double basePrice) {
        this.userId = userId;
        this.turfId = turfId;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.basePrice = basePrice;
    }

    /**
     * Works out which surge multiplier applies.
     * Weekend (Sat/Sun) and evening (slot starts at 18:00 or later) each have a multiplier.
     * If both apply we use the HIGHER one. If neither applies, the multiplier is 1.0.
     * It is static because it needs no Booking object, just the inputs.
     */
    public static double determineSurge(LocalDate date, String timeSlot,
                                        double weekendMultiplier, double eveningMultiplier) {
        double multiplier = 1.0;

        DayOfWeek day = date.getDayOfWeek();
        boolean weekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

        int startHour = Integer.parseInt(timeSlot.substring(0, 2));   // "18:00-19:00" -> 18
        boolean evening = (startHour >= 18);

        if (weekend) multiplier = Math.max(multiplier, weekendMultiplier);
        if (evening) multiplier = Math.max(multiplier, eveningMultiplier);
        return multiplier;
    }

    /** Applies the discount (use 0 for no promo) and calculates finalTotal. */
    public void calculateFinalTotal(double discountPercent) {
        double surgedPrice = basePrice * surgeMultiplier;
        this.discountAmount = surgedPrice * discountPercent / 100.0;
        this.finalTotal = surgedPrice - discountAmount;
    }

    public double getFinalTotal() { return finalTotal; }

    // ---- getters ----
    public int getBookingId()           { return bookingId; }
    public int getUserId()              { return userId; }
    public int getTurfId()              { return turfId; }
    public LocalDate getBookingDate()   { return bookingDate; }
    public String getTimeSlot()         { return timeSlot; }
    public double getBasePrice()        { return basePrice; }
    public double getSurgeMultiplier()  { return surgeMultiplier; }
    public String getPromoCode()        { return promoCode; }
    public double getDiscountAmount()   { return discountAmount; }
    public String getStatus()           { return status; }

    // ---- setters (only for values that legitimately change after creation) ----
    public void setBookingId(int bookingId)               { this.bookingId = bookingId; }
    public void setSurgeMultiplier(double surgeMultiplier){ this.surgeMultiplier = surgeMultiplier; }
    public void setPromoCode(String promoCode)            { this.promoCode = promoCode; }
    public void setStatus(String status)                  { this.status = status; }
}