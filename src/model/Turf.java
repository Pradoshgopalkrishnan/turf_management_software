package model;

/**
 * One turf (a playing ground that can be booked).
 * Has setters because the admin can edit a turf later.
 */
public class Turf {

    private int turfId;
    private String name;
    private String location;
    private int capacity;
    private double basePrice;   // price for one 1-hour slot

    public Turf(int turfId, String name, String location, int capacity, double basePrice) {
        this.turfId = turfId;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.basePrice = basePrice;
    }

    public int getTurfId()       { return turfId; }
    public String getName()      { return name; }
    public String getLocation()  { return location; }
    public int getCapacity()     { return capacity; }
    public double getBasePrice() { return basePrice; }

    public void setName(String name)           { this.name = name; }
    public void setLocation(String location)   { this.location = location; }
    public void setCapacity(int capacity)      { this.capacity = capacity; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    // A JComboBox shows toString() for each item, so this makes turfs display by name.
    @Override
    public String toString() {
        return name;
    }
}