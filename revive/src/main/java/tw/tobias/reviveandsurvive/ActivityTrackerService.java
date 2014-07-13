package tw.tobias.reviveandsurvive;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.content.Intent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ActivityTrackerService extends IntentService {
    public static final String STATUS_UNKNOWN = "Unknown";
    public static final String STATUS_IN_A_VEHICLE = "In a vehicle";
    public static final String STATUS_ON_FOOT = "On foot";
    public static final String STATUS_TILTING = "Tilting";
    public static final String STATUS_ON_BIKE = "On bike";
    public static final String STATUS_STILL = "Still";
    public static final String STATUS_RUNNING = "Running";

    public static final String BROADCAST_ACTION = "tw.tobias.reviveandsurvive";
    private static final String TAG = "ActivityTrackerService";
    public static final String SERIALIZED_HIST_KEY = "serializedHistory";
    private final Gson gson;
    private final Type listOfMeasurementObject;

    private List<Measurement> history;

    public SharedPreferences getSharedPreferences() {
        Context ctx = getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public ActivityTrackerService() {
        super("ActivityRecognitionService");

        gson = new Gson();
        listOfMeasurementObject = new TypeToken<List<Measurement>>(){}.getType();

        Log.d(TAG, "Initialized new ActivityTrackerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Create a new measurement as appropriate
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            addMeasurement(getActivityName(result.getMostProbableActivity().getType()), result.getTime());

            Log.d(TAG, getLatestMeasurement().toString());

            // Send an intent to users
            Intent uiIntent = new Intent(BROADCAST_ACTION);
            uiIntent.putExtra("currentState", getLatestStatus());
            uiIntent.putExtra("latestMeasurement", getLatestMeasurement().toJson());
            sendBroadcast(uiIntent);
        }
    }

    private String getActivityName(int detectedActivity){
        switch (detectedActivity) {
            case DetectedActivity.IN_VEHICLE:
                return STATUS_IN_A_VEHICLE;

//            case DetectedActivity.ON_FOOT:
//                return STATUS_ON_FOOT;
//            case DetectedActivity.ON_BICYCLE:
//                return STATUS_ON_BIKE;
//            case DetectedActivity.RUNNING:
//                return STATUS_RUNNING;
//
//            case DetectedActivity.STILL:
//                return STATUS_STILL;
//            case DetectedActivity.TILTING:
//                return STATUS_TILTING;
//
//            default:
//                Log.w(TAG, "Unknown activity type of " + detectedActivity);
//                return STATUS_UNKNOWN;

            default:
                return STATUS_ON_FOOT;
        }
    }

    public void addMeasurement(String status, long time) {
        loadHistory();

        time += 900000;

        Measurement latest = getLatestMeasurement();

        Log.i(TAG, "Previous status: " + (latest == null ? "null" : latest.status) + ", new status: " + status);

        if (latest != null && latest.status.equals(status)) {
            Log.d(TAG, "Reusing previous measurement");
            latest.updateMeasurement(time);
        }
        else {
            Log.d(TAG, "Creating new measurement");
            history.add(new Measurement(status, time));
        }
        getSharedPreferences().edit().putString(SERIALIZED_HIST_KEY, gson.toJson(history, listOfMeasurementObject)).apply();

        Log.i(TAG, "History length: " + history.size());
    }

    private void loadHistory () {
        if (history == null) {
            String serializedHistory = getSharedPreferences().getString(SERIALIZED_HIST_KEY, "[]");
            history = gson.fromJson(serializedHistory, listOfMeasurementObject);
        }
    }

    /**
     * Gets the latest measurement taken
     * @return Gets the latest Measurement taken, including its duration and how many times it's been updated
     */
    private Measurement getLatestMeasurement() {
        loadHistory();

        if (history.size() == 0) {
            return null;
        }

        return history.get(history.size() - 1);
    }

    public String getLatestStatus() {
        return getLatestMeasurement().status;
    }
}

