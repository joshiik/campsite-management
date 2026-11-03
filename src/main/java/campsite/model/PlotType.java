package campsite.model;

public enum PlotType {
    EMPTY_PITCH("Empty pitch"),
    STATIC_MOBILE_HOME("Static mobile home"),
    BUNGALOW("Bungalow"),
    APARTMENT("Apartment"),
    OTHER("Other");

    private final String displayName;

    PlotType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static PlotType fromText(String text) {
        String value = text.trim().replace(' ', '_').replace('-', '_');
        for (PlotType type : values()) {
            if (type.name().equalsIgnoreCase(value)
                    || type.displayName.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown plot type: " + text);
    }
}
