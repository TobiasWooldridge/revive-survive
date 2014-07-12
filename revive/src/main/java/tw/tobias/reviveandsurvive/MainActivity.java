package tw.tobias.reviveandsurvive;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showDrivingState(intent);
        }
    };

    private final static String TAG = "MainActivity";

    private final static int ONE_MINUTE = 60 * 1000;
    private final static int ONE_HOUR = 60 * 60 * 1000;
    private final static int TWO_HOURS = 2 * ONE_HOUR;

    private enum WarningLevel {
        NOT_DRIVING(0, "You're not driving. Keep an eye out for traffic and you should be pretty safe from road accidents :)"),

        SHORT_DRIVE(ONE_MINUTE, "You haven't been driving for very long. No problem!"),

        ADVISE_STOP(ONE_HOUR, "You should consider taking a break in a while"),

        WARNING((int)(1.5 * ONE_HOUR), "You should take a break soon"),

        DANGER(8 * ONE_HOUR, "You should take a break as soon as possible.");

        public final int millisAfter;
        public final String message;
        private WarningLevel(int millisAfter, String message) {
            this.millisAfter = millisAfter;
            this.message = message;
        }
    }

    static WarningLevel[] warningLevels = {
            WarningLevel.DANGER,
            WarningLevel.WARNING,
            WarningLevel.ADVISE_STOP,
            WarningLevel.SHORT_DRIVE,
            WarningLevel.NOT_DRIVING,
    };

    public WarningLevel getCurrentLevel(long drivingDuration) {
        for (WarningLevel level : warningLevels) {
            if (drivingDuration >= level.millisAfter) {
                return level;
            }
        }

        Log.e(TAG, "Weird, I didn't find something? Somebody broke warningLevels or secondsAfter was negative");
        return WarningLevel.NOT_DRIVING;
    }

    public void showWarningLevelMessage(WarningLevel level) {
        TextView guidanceMessage = (TextView)findViewById(R.id.guidance_message);
        guidanceMessage.setText(level.message);
    }

    public void updateDrivingProgressBar(long drivingDuration) {
        SeekBar drivingDurationBar = (SeekBar)findViewById(R.id.driving_duration_status_bar);
        drivingDurationBar.setProgress((int)(100 * (drivingDuration / TWO_HOURS)));
    }

    protected void showDrivingState(Intent intent) {
        TextView output = (TextView)findViewById(R.id.current_state);

        Measurement latest = Measurement.fromJson(intent.getStringExtra("latestMeasurement"));

        output.setText("You have been " + latest.status.toLowerCase() + " for " + latest.getDurationMillis()/1000 + "s");

        long drivingDuration = 0;
        if (latest.status == ActivityTrackerService.STATUS_IN_A_VEHICLE) {
            drivingDuration = latest.getDurationMillis();
        }

        updateDrivingProgressBar(drivingDuration);
        showWarningLevelMessage(getCurrentLevel(drivingDuration));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ActivityTrackerScan(this).startActivityRecognitionScan();

        showWarningLevelMessage(WarningLevel.NOT_DRIVING);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(ActivityTrackerService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
