package campsite.model;

import java.io.Serializable;

public final class Campsite implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String address;
    private final int stars;
    private final String contact;
    private final CustomList<String> facilities = new CustomList<>();
    private final CustomList<CampingArea> areas = new CustomList<>();

    public Campsite(int id, String name, String address, int stars, String contact,
            CustomList<String> facilities) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.stars = stars;
        this.contact = contact;
        for (String facility : facilities) {
            this.facilities.add(facility);
        }
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

    public int stars() {
        return stars;
    }

    public String contact() {
        return contact;
    }

    public CustomList<String> facilities() {
        return facilities;
    }

    public CustomList<CampingArea> areas() {
        return areas;
    }

    public boolean hasFacility(String name) {
        for (String facility : facilities) {
            if (facility.equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    public CampingArea findArea(String areaId) {
        for (CampingArea area : areas) {
            if (area.identifier().equalsIgnoreCase(areaId.trim())) {
                return area;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id + " - " + name + " (" + stars + " stars)";
    }
}
