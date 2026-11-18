package campsite.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CampsiteManagerTest {
    private CampsiteManager manager;
    private Campsite campsite;
    private Guest guest;
    private LocalDate start;
    private LocalDate end;

    @BeforeEach
    void setUp() {
        manager = new CampsiteManager();
        campsite = manager.addCampsite(101, "Green Lake", "Lake Road", 4, "01 234 567",
                CampsiteManager.parseFacilities("Pool, WiFi"));
        manager.addArea(campsite.id(), "Lakeside", "Pitches beside the lake");
        guest = manager.addGuest(7001, "Alex Green", LocalDate.of(1995, 4, 12),
                "1 Main Street", "01 222 333", "alex@example.com", "");
        start = LocalDate.now().plusDays(10);
        end = start.plusDays(5);
    }

    @Test
    void areaIdentifiersAreUniqueWithinTheirCampsite() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.addArea(campsite.id(), "lakeside", "Duplicate"));

        Campsite other = manager.addCampsite(205, "Hill Camp", "Hill Road", 3, "Contact",
                new CustomList<>());
        assertNotNull(manager.addArea(other.id(), "Lakeside", "Same name, different campsite"));
    }

    @Test
    void campsiteIdsAreUserProvidedPositiveAndUnique() {
        Campsite customId = manager.addCampsite(9001, "Custom ID Camp", "Hill Road",
                3, "Contact", new CustomList<>());

        assertEquals(9001, customId.id());
        assertThrows(IllegalArgumentException.class,
                () -> manager.addCampsite(9001, "Duplicate", "Address", 3, "Contact",
                        new CustomList<>()));
        assertThrows(IllegalArgumentException.class,
                () -> manager.addCampsite(0, "Invalid", "Address", 3, "Contact",
                        new CustomList<>()));
    }

    @Test
    void guestIdsAreUserProvidedPositiveAndUnique() {
        Guest customId = manager.addGuest(9001, "Custom ID Guest",
                LocalDate.of(1990, 1, 1), "", "", "", "");

        assertEquals(9001, customId.id());
        assertThrows(IllegalArgumentException.class,
                () -> manager.addGuest(9001, "Duplicate", LocalDate.of(1990, 1, 1),
                        "", "", "", ""));
        assertThrows(IllegalArgumentException.class,
                () -> manager.addGuest(0, "Invalid", LocalDate.of(1990, 1, 1),
                        "", "", "", ""));
    }

    @Test
    void bookingSelectsTheFirstPlotMatchingAllRequirements() {
        Plot unsuitable = addPlot("P1", PlotType.BUNGALOW, false, 1, 1, 20, 80);
        Plot suitable = addPlot("P2", PlotType.BUNGALOW, true, 2, 3, 45, 100);

        Booking booking = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(PlotType.BUNGALOW, PowerPreference.REQUIRED, 40, 1, 2, 120));

        assertEquals(suitable.identifier(), booking.plotId());
        assertTrue(unsuitable.bookings().isEmpty());
        assertEquals(1, suitable.bookings().size());
    }

    @Test
    void staticAccommodationRequiresPowerAndEnoughBedsForEveryGuest() {
        assertThrows(IllegalArgumentException.class,
                () -> addPlot("No power", PlotType.STATIC_MOBILE_HOME,
                        false, 1, 2, 30, 80));
        addPlot("Home", PlotType.STATIC_MOBILE_HOME, true, 1, 2, 30, 80);

        BookingRequest tooManyGuests = new BookingRequest(start, end,
                PlotType.STATIC_MOBILE_HOME, 3, PowerPreference.REQUIRED,
                0, 0, 0, 90);
        assertThrows(IllegalStateException.class,
                () -> manager.addBooking(campsite.id(), "Lakeside", guest.id(), tooManyGuests));

        BookingRequest fits = new BookingRequest(start, end,
                PlotType.STATIC_MOBILE_HOME, 2, PowerPreference.REQUIRED,
                0, 0, 0, 90);
        assertNotNull(manager.addBooking(campsite.id(), "Lakeside", guest.id(), fits));
    }

    @Test
    void overlappingDatesAreRejectedButBackToBackDatesAreAllowed() {
        Plot plot = addPlot("P1", PlotType.EMPTY_PITCH, true, 0, 0, 30, 40);
        BookingRequest request = request(null, PowerPreference.ANY, 0, 0, 0, 50);
        manager.addBooking(campsite.id(), "Lakeside", guest.id(), request);

        assertThrows(IllegalStateException.class,
                () -> manager.addBooking(campsite.id(), "Lakeside", guest.id(), request));
        Booking nextStay = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                new BookingRequest(end, end.plusDays(3), null, 1,
                        PowerPreference.ANY, 0, 0, 0, 50));

        assertEquals(plot.identifier(), nextStay.plotId());
    }

    @Test
    void cancellingBookingMakesItsDatesAvailableAgain() {
        addPlot("P1", PlotType.EMPTY_PITCH, false, 0, 0, 25, 35);
        Booking first = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(null, PowerPreference.ANY, 0, 0, 0, 40));

        assertTrue(manager.cancelBooking(first.id()));
        assertFalse(first.isPending());
        Booking replacement = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(null, PowerPreference.ANY, 0, 0, 0, 40));
        assertNotNull(replacement);
        assertFalse(manager.cancelBooking(first.id()));
    }

    @Test
    void removingPlotReassignsItsPendingBookingsWhenPossible() {
        Plot oldPlot = addPlot("P1", PlotType.BUNGALOW, true, 1, 2, 30, 90);
        Plot replacement = addPlot("P2", PlotType.BUNGALOW, true, 1, 2, 30, 95);
        Booking booking = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(PlotType.BUNGALOW, PowerPreference.REQUIRED, 0, 0, 0, 100));

        int cancelled = manager.removePlot(campsite.id(), "Lakeside", "P1");

        assertEquals(0, cancelled);
        assertTrue(oldPlot.bookings().isEmpty());
        assertEquals(booking, replacement.bookings().get(0));
        assertEquals("P2", booking.plotId());
    }

    @Test
    void smartBookingKeepsFacilityRequirementsWhenPlotIsRemoved() {
        Plot first = addPlot("P1", PlotType.BUNGALOW, true, 1, 2, 30, 90,
                "Air Conditioning");
        addPlot("P2", PlotType.BUNGALOW, true, 1, 2, 30, 90);
        Plot replacement = addPlot("P3", PlotType.BUNGALOW, true, 1, 2, 30, 95,
                "Air Conditioning");
        CustomList<String> requiredCampsite = CampsiteManager.parseFacilities("Pool");
        CustomList<String> requiredPlot = CampsiteManager.parseFacilities("Air Conditioning");
        BookingRequest request = new BookingRequest(start, end, PlotType.BUNGALOW,
                2, PowerPreference.ANY, 0, 0, 0, 100);
        PlotMatch match = manager.findSmartPlot(request, 4, requiredCampsite, requiredPlot);
        Booking booking = manager.bookSmartChoice(match, guest.id(), request);

        assertEquals(1, booking.request().requiredCampsiteFacilities().size());
        assertEquals(1, booking.request().requiredPlotFacilities().size());
        manager.removePlot(campsite.id(), "Lakeside", first.identifier());

        assertEquals("P3", booking.plotId());
        assertEquals(booking, replacement.bookings().get(0));
    }

    @Test
    void removingLastSuitablePlotCancelsItsPendingBooking() {
        addPlot("P1", PlotType.EMPTY_PITCH, false, 0, 0, 20, 30);
        Booking booking = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(null, PowerPreference.ANY, 0, 0, 0, 40));

        assertEquals(1, manager.removePlot(campsite.id(), "Lakeside", "P1"));
        assertTrue(booking.isCancelled());
        assertFalse(booking.isPending());
        assertTrue(manager.pendingBookingsForCampsite(campsite.id())
                .contains("Total pending bookings: 0"));
    }

    @Test
    void smartFindHonoursStarAndFacilityRequirements() {
        addPlot("P1", PlotType.BUNGALOW, true, 1, 2, 35, 90);
        CustomList<String> campsiteNeeds = CampsiteManager.parseFacilities("pool");
        CustomList<String> plotNeeds = CampsiteManager.parseFacilities("air conditioning");
        BookingRequest request = request(PlotType.BUNGALOW, PowerPreference.ANY, 0, 0, 0, 100);

        assertNull(manager.findSmartPlot(request, 5, campsiteNeeds, plotNeeds));
        addPlot("P2", PlotType.BUNGALOW, true, 1, 2, 35, 95, "Air Conditioning");
        PlotMatch match = manager.findSmartPlot(request, 4, campsiteNeeds, plotNeeds);

        assertNotNull(match);
        assertEquals("P2", match.plot().identifier());
        assertEquals(campsite.id(), match.campsite().id());
    }

    @Test
    void sampleDataProvidesAnAvailableSmartFindMatch() {
        CampsiteManager sample = CampsiteManager.createSampleData();
        LocalDate sampleStart = LocalDate.now().plusDays(30);
        BookingRequest request = new BookingRequest(sampleStart, sampleStart.plusDays(5),
                PlotType.BUNGALOW, 4, PowerPreference.ANY, 40, 2, 4, 200,
                CampsiteManager.parseFacilities("Pool"),
                CampsiteManager.parseFacilities("Kitchen, Wi-Fi"));

        PlotMatch match = sample.findSmartPlot(request, 4,
                request.requiredCampsiteFacilities(), request.requiredPlotFacilities());

        assertNotNull(match);
        assertEquals(202, match.campsite().id());
        assertEquals("PINE-02", match.plot().identifier());
        assertNotNull(sample.findGuest(5001));
    }

    @Test
    void guestRecordShowsPendingBookingAndGuestContactDetails() {
        addPlot("P1", PlotType.EMPTY_PITCH, false, 0, 0, 20, 30);
        Booking booking = manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(null, PowerPreference.ANY, 0, 0, 0, 40));

        String record = manager.guestRecord(guest.id());

        assertTrue(record.contains("Alex Green"));
        assertTrue(record.contains("alex@example.com"));
        assertTrue(record.contains("Booking " + booking.id()));
        assertTrue(record.contains("Pending"));
    }

    @Test
    void pendingBookingViewsShowHierarchyAndTotals() {
        addPlot("P1", PlotType.BUNGALOW, true, 1, 2, 30, 90);
        manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(PlotType.BUNGALOW, PowerPreference.ANY, 0, 0, 0, 100));

        String campsiteBookings = manager.pendingBookingsForCampsite(campsite.id());
        String systemBookings = manager.allPendingBookings();

        assertTrue(campsiteBookings.contains("Area: Lakeside"));
        assertTrue(campsiteBookings.contains("Plot: P1"));
        assertTrue(campsiteBookings.contains("Guests: 1"));
        assertTrue(campsiteBookings.contains("Total pending bookings in area: 1"));
        assertTrue(campsiteBookings.contains("Total pending bookings: 1"));
        assertTrue(systemBookings.contains("Total pending bookings at campsite: 1"));
        assertTrue(systemBookings.contains("Total pending bookings: 1"));
    }

    @Test
    void savingAndLoadingPreservesDataAndIdCounters(@TempDir Path directory) throws Exception {
        addPlot("P1", PlotType.EMPTY_PITCH, false, 0, 0, 20, 30);
        manager.addBooking(campsite.id(), "Lakeside", guest.id(),
                request(null, PowerPreference.ANY, 0, 0, 0, 40));
        Path file = directory.resolve("campsite-data.bin");

        manager.save(file);
        CampsiteManager loaded = CampsiteManager.load(file);

        assertEquals(1, loaded.campsites().size());
        assertEquals(101, loaded.campsites().get(0).id());
        assertEquals(1, loaded.bookings().size());
        assertTrue(loaded.allPendingBookings().contains("Alex Green"));
        assertEquals(8888, loaded.addGuest(8888, "Another Guest",
                LocalDate.of(1990, 1, 1), "", "", "", "").id());
    }

    @Test
    void resetClearsAllDataAndRestartsGeneratedIdentifiers() {
        manager.reset();

        assertTrue(manager.campsites().isEmpty());
        assertTrue(manager.guests().isEmpty());
        assertTrue(manager.bookings().isEmpty());
        assertEquals(42, manager.addGuest(42, "New Guest", LocalDate.of(1990, 1, 1),
                "", "", "", "").id());
    }

    private Plot addPlot(String id, PlotType type, boolean power, int bedrooms,
            int beds, double size, double cost) {
        return addPlot(id, type, power, bedrooms, beds, size, cost, "");
    }

    private Plot addPlot(String id, PlotType type, boolean power, int bedrooms,
            int beds, double size, double cost, String facilities) {
        return manager.addPlot(campsite.id(), "Lakeside", id, type, power,
                bedrooms, beds, size, CampsiteManager.parseFacilities(facilities),
                cost, "");
    }

    private BookingRequest request(PlotType type, PowerPreference power, double minSize,
            int minBedrooms, int minBeds, double budget) {
        return new BookingRequest(start, end, type, 1, power,
                minSize, minBedrooms, minBeds, budget);
    }
}
