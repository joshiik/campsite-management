package campsite.model;

public final class PlotMatch {
    private final Campsite campsite;
    private final CampingArea area;
    private final Plot plot;
    private final CustomList<String> requiredCampsiteFacilities;
    private final CustomList<String> requiredPlotFacilities;

    PlotMatch(Campsite campsite, CampingArea area, Plot plot,
            CustomList<String> requiredCampsiteFacilities,
            CustomList<String> requiredPlotFacilities) {
        this.campsite = campsite;
        this.area = area;
        this.plot = plot;
        this.requiredCampsiteFacilities = copy(requiredCampsiteFacilities);
        this.requiredPlotFacilities = copy(requiredPlotFacilities);
    }

    public Campsite campsite() {
        return campsite;
    }

    public CampingArea area() {
        return area;
    }

    public Plot plot() {
        return plot;
    }

    CustomList<String> requiredCampsiteFacilities() {
        return copy(requiredCampsiteFacilities);
    }

    CustomList<String> requiredPlotFacilities() {
        return copy(requiredPlotFacilities);
    }

    private static CustomList<String> copy(CustomList<String> values) {
        CustomList<String> result = new CustomList<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }
}
