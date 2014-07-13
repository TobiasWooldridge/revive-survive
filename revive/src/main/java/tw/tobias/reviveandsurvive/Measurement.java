package tw.tobias.reviveandsurvive;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Measurement {
    private static final Type measurementType = new TypeToken<Measurement>(){}. getType();

    long since;
    long lastUpdated;
    String status;
    int measurements;

    public Measurement(String status, long time) {
        this.status = status;
        this.since = time;
        this.lastUpdated = time;
        measurements = 1;
    }

    public void updateMeasurement(long time) {
        lastUpdated = time;
        measurements++;
    }

    public long getDurationMillis() {
        return lastUpdated  - since;
    }

    public String getDurationFormatted() {
        long totalMillis = getDurationMillis();

        int seconds = (int)(totalMillis / 1000);

        int hours = seconds / (3600);
        seconds = seconds % 3600;

        int minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "since=" + since +
                ", lastUpdated=" + lastUpdated +
                ", status='" + status + '\'' +
                ", measurements=" + measurements +
                ", duration=" + getDurationFormatted() +
                '}';
    }

    public boolean inVehicle() {
        return status.equals(ActivityTrackerService.STATUS_IN_A_VEHICLE);
    }

    public static Measurement fromJson(String serialized) {
        return new Gson().fromJson(serialized, measurementType);
    }

    public String toJson() {
        return new Gson().toJson(this, measurementType);
    }
}
