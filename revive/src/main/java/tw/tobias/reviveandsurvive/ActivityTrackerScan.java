package tw.tobias.reviveandsurvive;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import android.content.Intent;

public class ActivityTrackerScan implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{
    private Context context;
    private static final String TAG = "ActivityTrackerScan";
    private static ActivityRecognitionClient mActivityRecognitionClient;
    private static PendingIntent callbackIntent;

    public ActivityTrackerScan(Context context) {
        this.context = context;
    }

    public void startActivityRecognitionScan() {
        Log.i(TAG, "Starting activity recognition client");
        mActivityRecognitionClient = new ActivityRecognitionClient(context, this, this);
        mActivityRecognitionClient.connect();
    }

    public void stopActivityRecognitionScan() {
        try{
            mActivityRecognitionClient.removeActivityUpdates(callbackIntent);
        } catch (IllegalStateException e){
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    /**
     * Connection established - start listening now
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connected to activity tracker service");
        Intent intent = new Intent(context, ActivityTrackerService.class);
        callbackIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mActivityRecognitionClient.requestActivityUpdates(0, callbackIntent); // TODO: Change from 0 to sane value
    }

    @Override
    public void onDisconnected() {
    }

}

