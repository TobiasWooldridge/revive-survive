package tw.tobias.reviveandsurvive.client;

public class PitStop {
    public static final String PETROL_TYPE = "PETROL";
    public static final String TOILET_TYPE = "TOILET";
    public static final String BBQ_TYPE = "BBQ";
    public static final String REST_TYPE = "REST";
    private double lat;
    private double lon;
    private double distance = 0;
    private String type = "";
    private String name = "";

    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
    public String getName() {
        if (name.equals("Rest Area")) {
            return "Public designated rest stop";
        }
        else if (name.equals("")) {
            return  String.format("%s, %s", lat, lon);
        }
        return name;
    }
    public String getGlyph() {
        switch (type) {
            case PETROL_TYPE:
                return "\u26FD";
            case TOILET_TYPE:
                return "\uD83D\uDEBB";
            case BBQ_TYPE:
                return "\uD83C\uDF7D";
            case REST_TYPE:
                return "\u2615";
            default:
                return "?";
        }
    }
    public String getRawType() {
        return type;
    }
    public String getType() {
        switch (type) {
            case PETROL_TYPE:
                return "Petrol station";
            case TOILET_TYPE:
                return "Public toilet";
            case BBQ_TYPE:
                return "Public BBQ";
            case REST_TYPE:
                return "Rest stop";
            default:
                return type;
        }
    }
    public double getDistance() {
        return distance;
    }

    public String toString() {
        return getType() + " " + getName();
    }
}
