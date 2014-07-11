package tw.tobias.reviveandsurvive;


import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class ActivityTrackerService extends IntentService {
    private static final String TAG = "ActivityTrackerService";
    public static final String BROADCAST_ACTION = "tw.tobias.reviveandsurvive";
    Intent uiIntent;

    private List<String> history;

    public ActivityTrackerService() {
        super("ActivityRecognitionService");

        Log.d(TAG, "Initialized");

        history = new ArrayList<>();
        uiIntent = new Intent(BROADCAST_ACTION);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            history.add(getActivityName(result.getMostProbableActivity().getType()));

            Log.d(TAG, getLatestStatus() + " with confidence " + result.getActivityConfidence(result.getMostProbableActivity().getType()));

            Log.d(TAG, "Time: " + result.getTime());

            uiIntent.putExtra("currentState", getLatestStatus());
            sendBroadcast(uiIntent);
        }
    }

    private String getActivityName(int detectedActivity){
        switch (detectedActivity) {
            case DetectedActivity.IN_VEHICLE:
                return "In vehicle";
            case DetectedActivity.ON_FOOT:
                return "On foot";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.ON_BICYCLE:
                return "On bike";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.RUNNING:
                return "Running";
            default:
                return "Unknown";
        }
    }

    public String getLatestStatus() {
        return history.get(history.size() - 1);
    }
}

