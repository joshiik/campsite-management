package campsite.model;

import java.io.Serializable;
import java.time.LocalDate;

public final class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final int guestId;
    private final BookingRequest request;
    private int campsiteId;
    private String areaId;
    private String plotId;
    private boolean cancelled;

    public Booking(int id, int guestId, int campsiteId, String areaId,
            String plotId, BookingRequest request) {
        this.id = id;
        this.guestId = guestId;
        this.campsiteId = campsiteId;
        this.areaId = areaId;
        this.plotId = plotId;
        this.request = request;
    }

    public int id() {
        return id;
    }

    public int guestId() {
        return guestId;
    }

    public int campsiteId() {
        return campsiteId;
    }

    public String areaId() {
        return areaId;
    }

    public String plotId() {
        return plotId;
    }

    public BookingRequest request() {
        return request;
    }

    public LocalDate startDate() {
        return request.startDate();
    }

    public LocalDate endDate() {
        return request.endDate();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isPending() {
        return !cancelled && !endDate().isBefore(LocalDate.now());
    }

    public String status() {
        if (cancelled) {
            return "Cancelled";
        }
        return isPending() ? "Pending" : "Past";
    }

    void moveTo(int newCampsiteId, String newAreaId, String newPlotId) {
        campsiteId = newCampsiteId;
        areaId = newAreaId;
        plotId = newPlotId;
    }

    void cancel() {
        cancelled = true;
    }

    @Override
    public String toString() {
        return "Booking " + id + " - " + startDate() + " to " + endDate();
    }
}
