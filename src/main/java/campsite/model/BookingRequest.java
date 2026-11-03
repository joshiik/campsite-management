package campsite.model;

import java.io.Serializable;
import java.time.LocalDate;

public final class BookingRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final PlotType preferredType;
    private final int guestCount;
    private final PowerPreference powerPreference;
    private final double minimumSize;
    private final int minimumBedrooms;
    private final int minimumBeds;
    private final double budgetPerNight;
    private final CustomList<String> requiredCampsiteFacilities;
    private final CustomList<String> requiredPlotFacilities;

    public BookingRequest(LocalDate startDate, LocalDate endDate, PlotType preferredType,
            int guestCount, PowerPreference powerPreference, double minimumSize,
            int minimumBedrooms, int minimumBeds, double budgetPerNight) {
        this(startDate, endDate, preferredType, guestCount, powerPreference,
                minimumSize, minimumBedrooms, minimumBeds, budgetPerNight,
                new CustomList<>(), new CustomList<>());
    }

    public BookingRequest(LocalDate startDate, LocalDate endDate, PlotType preferredType,
            int guestCount, PowerPreference powerPreference, double minimumSize,
            int minimumBedrooms, int minimumBeds, double budgetPerNight,
            CustomList<String> requiredCampsiteFacilities,
            CustomList<String> requiredPlotFacilities) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (guestCount < 1 || !Double.isFinite(minimumSize) || minimumSize < 0
                || minimumBedrooms < 0 || minimumBeds < 0
                || !Double.isFinite(budgetPerNight) || budgetPerNight < 0) {
            throw new IllegalArgumentException("Guest count must be positive; other requirements cannot be negative.");
        }
        if (powerPreference == null) {
            throw new IllegalArgumentException("Power preference is required.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.preferredType = preferredType;
        this.guestCount = guestCount;
        this.powerPreference = powerPreference;
        this.minimumSize = minimumSize;
        this.minimumBedrooms = minimumBedrooms;
        this.minimumBeds = minimumBeds;
        this.budgetPerNight = budgetPerNight;
        this.requiredCampsiteFacilities = copy(requiredCampsiteFacilities);
        this.requiredPlotFacilities = copy(requiredPlotFacilities);
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public PlotType preferredType() {
        return preferredType;
    }

    public int guestCount() {
        return guestCount;
    }

    public PowerPreference powerPreference() {
        return powerPreference;
    }

    public double minimumSize() {
        return minimumSize;
    }

    public int minimumBedrooms() {
        return minimumBedrooms;
    }

    public int minimumBeds() {
        return minimumBeds;
    }

    public double budgetPerNight() {
        return budgetPerNight;
    }

    public CustomList<String> requiredCampsiteFacilities() {
        return copy(requiredCampsiteFacilities);
    }

    public CustomList<String> requiredPlotFacilities() {
        return copy(requiredPlotFacilities);
    }

    public BookingRequest withAdditionalFacilities(CustomList<String> campsiteFacilities,
            CustomList<String> plotFacilities) {
        return new BookingRequest(startDate, endDate, preferredType, guestCount,
                powerPreference, minimumSize, minimumBedrooms, minimumBeds, budgetPerNight,
                combine(requiredCampsiteFacilities, campsiteFacilities),
                combine(requiredPlotFacilities, plotFacilities));
    }

    private static CustomList<String> combine(CustomList<String> first,
            CustomList<String> second) {
        CustomList<String> result = copy(first);
        if (second != null) {
            for (String facility : second) {
                boolean alreadyAdded = false;
                for (String existing : result) {
                    if (existing.equalsIgnoreCase(facility.trim())) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    result.add(facility);
                }
            }
        }
        return result;
    }

    private static CustomList<String> copy(CustomList<String> values) {
        CustomList<String> result = new CustomList<>();
        if (values != null) {
            for (String value : values) {
                result.add(value);
            }
        }
        return result;
    }
}
