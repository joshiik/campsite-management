package campsite.model;

import java.io.Serializable;

public final class Plot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String identifier;
    private final PlotType type;
    private final boolean hasPower;
    private final int bedrooms;
    private final int beds;
    private final double size;
    private final CustomList<String> facilities = new CustomList<>();
    private final double costPerNight;
    private final String photoUrl;
    private final CustomList<Booking> bookings = new CustomList<>();

    public Plot(String identifier, PlotType type, boolean hasPower, int bedrooms, int beds,
            double size, CustomList<String> facilities, double costPerNight, String photoUrl) {
        this.identifier = identifier;
        this.type = type;
        this.hasPower = hasPower;
        this.bedrooms = bedrooms;
        this.beds = beds;
        this.size = size;
        this.costPerNight = costPerNight;
        this.photoUrl = photoUrl;
        for (String facility : facilities) {
            this.facilities.add(facility);
        }
    }

    public String identifier() {
        return identifier;
    }

    public PlotType type() {
        return type;
    }

    public boolean hasPower() {
        return hasPower;
    }

    public int bedrooms() {
        return bedrooms;
    }

    public int beds() {
        return beds;
    }

    public double size() {
        return size;
    }

    public CustomList<String> facilities() {
        return facilities;
    }

    public double costPerNight() {
        return costPerNight;
    }

    public String photoUrl() {
        return photoUrl;
    }

    public CustomList<Booking> bookings() {
        return bookings;
    }

    public boolean hasFacility(String name) {
        for (String facility : facilities) {
            if (facility.equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return identifier + " - " + type.displayName() + ", €" + costPerNight + "/night";
    }
}
