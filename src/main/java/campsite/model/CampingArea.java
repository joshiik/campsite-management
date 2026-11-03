package campsite.model;

import java.io.Serializable;

public final class CampingArea implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String identifier;
    private final String description;
    private final CustomList<Plot> plots = new CustomList<>();

    public CampingArea(String identifier, String description) {
        this.identifier = identifier;
        this.description = description;
    }

    public String identifier() {
        return identifier;
    }

    public String description() {
        return description;
    }

    public CustomList<Plot> plots() {
        return plots;
    }

    public Plot findPlot(String plotId) {
        for (Plot plot : plots) {
            if (plot.identifier().equalsIgnoreCase(plotId.trim())) {
                return plot;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return identifier + " (" + plots.size() + " plots)";
    }
}
