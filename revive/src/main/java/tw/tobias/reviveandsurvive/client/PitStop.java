package tw.tobias.reviveandsurvive.client;

public class PitStop {
    private double lat;
    private double lon;
    private double distance;
    private String type;
    private String description;

    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
    public String getType() {
        return type;
    }
    public String getDescription() {
        return description;
    }
    public double getDistance() {
        return distance;
    }

    public String toString() {
        return getType() + " " + getDescription();
    }
}
