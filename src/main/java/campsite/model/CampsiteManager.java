package campsite.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

public final class CampsiteManager implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final CustomList<Campsite> campsites = new CustomList<>();
    private final CustomList<Guest> guests = new CustomList<>();
    private final CustomList<Booking> bookings = new CustomList<>();
    private int nextBookingId = 1;

    public CustomList<Campsite> campsites() {
        return campsites;
    }

    public CustomList<Guest> guests() {
        return guests;
    }

    public CustomList<Booking> bookings() {
        return bookings;
    }

    public Campsite addCampsite(int id, String name, String address, int stars, String contact,
            CustomList<String> facilities) {
        if (id <= 0) {
            throw new IllegalArgumentException("Campsite ID must be a positive number.");
        }
        if (findCampsite(id) != null) {
            throw new IllegalArgumentException("Campsite ID " + id + " already exists.");
        }
        requireText(name, "Campsite name");
        requireText(address, "Address");
        requireText(contact, "Contact information");
        if (facilities == null) {
            throw new IllegalArgumentException("Campsite facilities are required.");
        }
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Campsite stars must be between 1 and 5.");
        }
        Campsite campsite = new Campsite(id, name.trim(), address.trim(),
                stars, contact.trim(), facilities);
        campsites.add(campsite);
        return campsite;
    }

    public CampingArea addArea(int campsiteId, String identifier, String description) {
        Campsite campsite = requireCampsite(campsiteId);
        requireText(identifier, "Area identifier");
        requireText(description, "Area description");
        if (campsite.findArea(identifier) != null) {
            throw new IllegalArgumentException("Area identifiers must be unique within a campsite.");
        }
        CampingArea area = new CampingArea(identifier.trim(), description.trim());
        campsite.areas().add(area);
        return area;
    }

    public Plot addPlot(int campsiteId, String areaId, String identifier, PlotType type,
            boolean hasPower, int bedrooms, int beds, double size,
            CustomList<String> facilities, double costPerNight, String photoUrl) {
        CampingArea area = requireArea(campsiteId, areaId);
        requireText(identifier, "Plot identifier");
        if (area.findPlot(identifier) != null) {
            throw new IllegalArgumentException("Plot identifiers must be unique within a camping area.");
        }
        if (type == null || bedrooms < 0 || beds < 0 || !Double.isFinite(size)
                || size < 0 || !Double.isFinite(costPerNight) || costPerNight < 0) {
            throw new IllegalArgumentException("Plot details cannot be negative or missing.");
        }
        if (type == PlotType.STATIC_MOBILE_HOME && !hasPower) {
            throw new IllegalArgumentException("Static dwellings must have power.");
        }
        if (facilities == null) {
            throw new IllegalArgumentException("Plot facilities are required.");
        }
        Plot plot = new Plot(identifier.trim(), type, hasPower, bedrooms, beds, size,
                facilities, costPerNight, photoUrl == null ? "" : photoUrl.trim());
        area.plots().add(plot);
        return plot;
    }

    public Guest addGuest(int id, String name, LocalDate dateOfBirth, String address,
            String telephone, String email, String notes) {
        if (id <= 0) {
            throw new IllegalArgumentException("Guest ID must be a positive number.");
        }
        if (findGuest(id) != null) {
            throw new IllegalArgumentException("Guest ID " + id + " already exists.");
        }
        requireText(name, "Guest name");
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Guest date of birth must not be in the future.");
        }
        Guest guest = new Guest(id, name.trim(), dateOfBirth,
                clean(address), clean(telephone), clean(email), clean(notes));
        guests.add(guest);
        return guest;
    }

    public Booking addBooking(int campsiteId, String areaId, int guestId,
            BookingRequest request) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            throw new IllegalArgumentException("Guest " + guestId + " does not exist.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Booking requirements are required.");
        }
        Campsite campsite = requireCampsite(campsiteId);
        CampingArea area = requireArea(campsiteId, areaId);
        if (!hasAllFacilities(campsite.facilities(),
                request.requiredCampsiteFacilities())) {
            throw new IllegalStateException("The campsite does not have all required facilities.");
        }
        Plot plot = findAvailablePlot(campsite, area, request);
        if (plot == null) {
            throw new IllegalStateException("No available plot matches every booking requirement.");
        }
        return createBooking(campsite, area, plot, guest, request);
    }

    public PlotMatch findSmartPlot(BookingRequest request, int minimumStars,
            CustomList<String> requiredCampsiteFacilities,
            CustomList<String> requiredPlotFacilities) {
        if (request == null || requiredCampsiteFacilities == null
                || requiredPlotFacilities == null || minimumStars < 1 || minimumStars > 5) {
            throw new IllegalArgumentException("A booking request and 1-5 minimum stars are required.");
        }
        PlotMatch best = null;
        for (Campsite campsite : campsites) {
            if (campsite.stars() < minimumStars
                    || !hasAllFacilities(campsite.facilities(), requiredCampsiteFacilities)) {
                continue;
            }
            for (CampingArea area : campsite.areas()) {
                for (Plot plot : area.plots()) {
                    if (!matches(plot, request)
                            || hasBookingConflict(plot, request.startDate(), request.endDate())
                            || !hasAllFacilities(campsite.facilities(),
                                    request.requiredCampsiteFacilities())
                            || !hasAllFacilities(plot.facilities(), requiredPlotFacilities)) {
                        continue;
                    }
                    if (isBetterSmartChoice(campsite, plot, best)) {
                        best = new PlotMatch(campsite, area, plot,
                                requiredCampsiteFacilities, requiredPlotFacilities);
                    }
                }
            }
        }
        return best;
    }

    public Booking bookSmartChoice(PlotMatch match, int guestId, BookingRequest request) {
        if (match == null || request == null || findGuest(guestId) == null) {
            throw new IllegalArgumentException("A valid match, guest and booking request are required.");
        }
        Campsite campsite = requireCampsite(match.campsite().id());
        CampingArea area = requireArea(campsite.id(), match.area().identifier());
        Plot plot = area.findPlot(match.plot().identifier());
        BookingRequest completeRequest = request.withAdditionalFacilities(
                match.requiredCampsiteFacilities(), match.requiredPlotFacilities());
        if (plot == null || !matches(plot, completeRequest)
                || !hasAllFacilities(campsite.facilities(),
                        completeRequest.requiredCampsiteFacilities())
                || hasBookingConflict(plot, request.startDate(), request.endDate())) {
            throw new IllegalStateException("The suggested plot is no longer available.");
        }
        return createBooking(campsite, area, plot, findGuest(guestId), completeRequest);
    }

    public boolean cancelBooking(int bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking == null || !booking.isPending()) {
            return false;
        }
        Plot plot = findPlot(booking.campsiteId(), booking.areaId(), booking.plotId());
        if (plot != null) {
            plot.bookings().remove(booking);
        }
        booking.cancel();
        return true;
    }

    public int removePlot(int campsiteId, String areaId, String plotId) {
        Campsite campsite = requireCampsite(campsiteId);
        CampingArea area = requireArea(campsiteId, areaId);
        Plot removed = area.findPlot(plotId);
        if (removed == null) {
            throw new IllegalArgumentException("Plot does not exist.");
        }

        int cancelledCount = 0;
        CustomList<Booking> previousBookings = new CustomList<>();
        for (Booking booking : removed.bookings()) {
            previousBookings.add(booking);
        }
        for (Booking booking : previousBookings) {
            if (!booking.isPending()) {
                continue;
            }
            PlotMatch replacement = findReplacement(campsite, removed, booking.request());
            removed.bookings().remove(booking);
            if (replacement == null) {
                booking.cancel();
                cancelledCount++;
            } else {
                replacement.plot().bookings().add(booking);
                booking.moveTo(campsite.id(), replacement.area().identifier(),
                        replacement.plot().identifier());
            }
        }
        area.plots().remove(removed);
        return cancelledCount;
    }

    public Guest findGuest(int guestId) {
        for (Guest guest : guests) {
            if (guest.id() == guestId) {
                return guest;
            }
        }
        return null;
    }

    public Campsite findCampsite(int campsiteId) {
        for (Campsite campsite : campsites) {
            if (campsite.id() == campsiteId) {
                return campsite;
            }
        }
        return null;
    }

    public Booking findBooking(int bookingId) {
        for (Booking booking : bookings) {
            if (booking.id() == bookingId) {
                return booking;
            }
        }
        return null;
    }

    public CustomList<Booking> guestBookings(int guestId) {
        CustomList<Booking> result = new CustomList<>();
        for (Booking booking : bookings) {
            if (booking.guestId() == guestId) {
                result.add(booking);
            }
        }
        return result;
    }

    public String pendingBookingsForCampsite(int campsiteId) {
        Campsite campsite = requireCampsite(campsiteId);
        StringBuilder result = new StringBuilder();
        result.append("Campsite ").append(campsite.id()).append(": ")
                .append(campsite.name()).append('\n');
        int total = appendCampsiteBookings(campsite, result);
        result.append("\nTotal pending bookings: ").append(total);
        return result.toString();
    }

    public String allPendingBookings() {
        StringBuilder result = new StringBuilder();
        int total = 0;
        for (Campsite campsite : campsites) {
            result.append("\nCampsite ").append(campsite.id()).append(": ")
                    .append(campsite.name()).append('\n');
            int campsiteTotal = appendCampsiteBookings(campsite, result);
            result.append("  Total pending bookings at campsite: ")
                    .append(campsiteTotal).append('\n');
            total += campsiteTotal;
        }
        result.append("\nTotal pending bookings: ").append(total);
        return result.toString();
    }

    public String guestRecord(int guestId) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            throw new IllegalArgumentException("Guest " + guestId + " does not exist.");
        }
        StringBuilder result = new StringBuilder();
        result.append("Guest ").append(guest.id()).append(": ").append(guest.name())
                .append("\nDate of birth: ").append(guest.dateOfBirth())
                .append("\nAddress: ").append(guest.address())
                .append("\nTelephone: ").append(guest.telephone())
                .append("\nEmail: ").append(guest.email())
                .append("\nNotes: ").append(guest.notes()).append("\n\nBookings:\n");
        int count = 0;
        for (Booking booking : bookings) {
            if (booking.guestId() == guestId) {
                result.append("Booking ").append(booking.id()).append(" | ")
                        .append(booking.status()).append(" | campsite ")
                        .append(booking.campsiteId()).append(" | ")
                        .append(booking.areaId()).append(" | plot ")
                        .append(booking.plotId()).append(" | ")
                        .append(booking.startDate()).append(" to ")
                        .append(booking.endDate()).append('\n');
                count++;
            }
        }
        if (count == 0) {
            result.append("No bookings.\n");
        }
        return result.toString();
    }

    public void reset() {
        campsites.clear();
        guests.clear();
        bookings.clear();
        nextBookingId = 1;
    }

    public void save(Path file) throws IOException {
        Path absoluteFile = file.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporaryFile = absoluteFile.resolveSibling(absoluteFile.getFileName() + ".tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporaryFile))) {
            output.writeObject(this);
        }
        try {
            Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static CampsiteManager load(Path file) throws IOException {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            Object savedData = input.readObject();
            if (savedData instanceof CampsiteManager manager) {
                return manager;
            }
            throw new IOException("The selected file does not contain campsite data.");
        } catch (ClassNotFoundException exception) {
            throw new IOException("The saved campsite data uses an unknown class.", exception);
        }
    }

    public static CustomList<String> parseFacilities(String text) {
        CustomList<String> facilities = new CustomList<>();
        if (text == null || text.isBlank()) {
            return facilities;
        }
        StringBuilder value = new StringBuilder();
        for (int index = 0; index <= text.length(); index++) {
            char character = index == text.length() ? ',' : text.charAt(index);
            if (character == ',') {
                String facility = value.toString().trim();
                if (!facility.isEmpty() && !containsIgnoreCase(facilities, facility)) {
                    facilities.add(facility);
                }
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        return facilities;
    }

    private Booking createBooking(Campsite campsite, CampingArea area, Plot plot,
            Guest guest, BookingRequest request) {
        Booking booking = new Booking(nextBookingId++, guest.id(), campsite.id(),
                area.identifier(), plot.identifier(), request);
        plot.bookings().add(booking);
        bookings.add(booking);
        return booking;
    }

    private Plot findAvailablePlot(Campsite campsite, CampingArea area,
            BookingRequest request) {
        if (!hasAllFacilities(campsite.facilities(), request.requiredCampsiteFacilities())) {
            return null;
        }
        for (Plot plot : area.plots()) {
            if (matches(plot, request)
                    && !hasBookingConflict(plot, request.startDate(), request.endDate())) {
                return plot;
            }
        }
        return null;
    }

    private boolean matches(Plot plot, BookingRequest request) {
        if (request.preferredType() != null && plot.type() != request.preferredType()) {
            return false;
        }
        if (request.powerPreference() == PowerPreference.REQUIRED && !plot.hasPower()) {
            return false;
        }
        if (request.powerPreference() == PowerPreference.NOT_REQUIRED && plot.hasPower()) {
            return false;
        }
        return plot.size() >= request.minimumSize()
                && plot.bedrooms() >= request.minimumBedrooms()
                && plot.beds() >= request.minimumBeds()
                && plot.costPerNight() <= request.budgetPerNight()
                && (plot.type() == PlotType.EMPTY_PITCH
                    || plot.beds() >= request.guestCount())
                && hasAllFacilities(plot.facilities(), request.requiredPlotFacilities());
    }

    private boolean hasBookingConflict(Plot plot, LocalDate start, LocalDate end) {
        for (Booking booking : plot.bookings()) {
            if (booking.isPending() && start.isBefore(booking.endDate())
                    && end.isAfter(booking.startDate())) {
                return true;
            }
        }
        return false;
    }

    private PlotMatch findReplacement(Campsite campsite, Plot removedPlot,
            BookingRequest request) {
        for (CampingArea area : campsite.areas()) {
            for (Plot plot : area.plots()) {
                if (plot != removedPlot && matches(plot, request)
                        && !hasBookingConflict(plot, request.startDate(), request.endDate())) {
                    return new PlotMatch(campsite, area, plot,
                            request.requiredCampsiteFacilities(),
                            request.requiredPlotFacilities());
                }
            }
        }
        return null;
    }

    private boolean isBetterSmartChoice(Campsite campsite, Plot plot, PlotMatch current) {
        if (current == null || campsite.stars() > current.campsite().stars()) {
            return true;
        }
        return campsite.stars() == current.campsite().stars()
                && plot.costPerNight() < current.plot().costPerNight();
    }

    private boolean hasAllFacilities(CustomList<String> available,
            CustomList<String> required) {
        for (String requiredFacility : required) {
            if (!containsIgnoreCase(available, requiredFacility)) {
                return false;
            }
        }
        return true;
    }

    private int appendCampsiteBookings(Campsite campsite, StringBuilder result) {
        int total = 0;
        for (CampingArea area : campsite.areas()) {
            result.append("  Area: ").append(area.identifier()).append('\n');
            int areaTotal = 0;
            for (Plot plot : area.plots()) {
                result.append("    Plot: ").append(plot.identifier()).append('\n');
                int count = 0;
                for (Booking booking : plot.bookings()) {
                    if (booking.isPending()) {
                        Guest guest = findGuest(booking.guestId());
                        result.append("      Booking ").append(booking.id())
                                .append(" | Guest ")
                                .append(guest == null ? booking.guestId() : guest.name())
                                .append(" | Type: ").append(plot.type().displayName())
                                .append(" | Guests: ").append(booking.request().guestCount())
                                .append(" | ").append(booking.startDate()).append(" to ")
                                .append(booking.endDate())
                                .append(" | €").append(plot.costPerNight()).append("/night")
                                .append(" | Stay total: €")
                                .append(plot.costPerNight()
                                        * java.time.temporal.ChronoUnit.DAYS.between(
                                                booking.startDate(), booking.endDate()))
                                .append('\n');
                        count++;
                        total++;
                        areaTotal++;
                    }
                }
                result.append("      Pending bookings on plot: ").append(count).append('\n');
            }
            result.append("    Total pending bookings in area: ")
                    .append(areaTotal).append('\n');
        }
        return total;
    }

    private Campsite requireCampsite(int campsiteId) {
        Campsite campsite = findCampsite(campsiteId);
        if (campsite == null) {
            throw new IllegalArgumentException("Campsite " + campsiteId + " does not exist.");
        }
        return campsite;
    }

    private CampingArea requireArea(int campsiteId, String areaId) {
        CampingArea area = requireCampsite(campsiteId).findArea(areaId);
        if (area == null) {
            throw new IllegalArgumentException("Camping area does not exist.");
        }
        return area;
    }

    private Plot findPlot(int campsiteId, String areaId, String plotId) {
        Campsite campsite = findCampsite(campsiteId);
        if (campsite == null) {
            return null;
        }
        CampingArea area = campsite.findArea(areaId);
        return area == null ? null : area.findPlot(plotId);
    }

    private static boolean containsIgnoreCase(CustomList<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
