package tw.tobias.reviveandsurvive;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import org.w3c.dom.Text;

import java.util.Locale;

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
    private TextToSpeech tts;

    public class TTSInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            // Do nothing #yolo
            Log.d(TAG, "Initialized TTS with status " + status);
            tts.setLanguage(Locale.US);
        }
    }

    private enum WarningLevel {
        NOT_DRIVING(0, "You're not driving. Keep an eye out for traffic and you should be pretty safe from road accidents :)", "No time at all"),

        SHORT_DRIVE(ONE_MINUTE, "You haven't been driving for very long. No problem!", "One minute"),

        ADVISE_STOP(ONE_HOUR, "You should consider taking a break in a while", "One hour"),

        WARNING((int)(1.5 * ONE_HOUR), "You should take a break soon", "One and a half hours"),

        DANGER(2 * ONE_HOUR, "You should take a break.", "Two hours"),

        IMMEDIATE_DANGER(8 * ONE_HOUR, "You should take a break.", "Eight hours");

        public final int millisAfter;
        public final String message;
        public final String timeDesc;

        private WarningLevel(int millisAfter, String message, String timeDesc) {
            this.millisAfter = millisAfter;
            this.message = message;
            this.timeDesc = timeDesc;
        }
    }

    static WarningLevel[] warningLevels = {
            WarningLevel.IMMEDIATE_DANGER,
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


    public WarningLevel previousLevel = null;
    public void showWarningLevelMessage(WarningLevel level) {
        TextView guidanceMessage = (TextView)findViewById(R.id.guidance_message);
        guidanceMessage.setText(level.message);

        if (previousLevel != level) {
            switch(level) {
                case WARNING:
                case DANGER:
                case IMMEDIATE_DANGER:
                    tts.speak("You have been driving for over " + level.timeDesc + ". " + level.message, TextToSpeech.QUEUE_ADD, null);
                    break;
                default:
                    break;
            }
        }

        previousLevel = level;
    }

    public void updateDrivingProgressBar(long drivingDuration) {
        SeekBar drivingDurationBar = (SeekBar)findViewById(R.id.driving_duration_status_bar);
        drivingDurationBar.setEnabled(false);
        drivingDurationBar.setProgress((int)(100 * (drivingDuration / TWO_HOURS)));
    }

    private void showDrivingState(Intent intent) {
        TextView output = (TextView)findViewById(R.id.current_state);

        Measurement latest = Measurement.fromJson(intent.getStringExtra("latestMeasurement"));

        output.setText("You have been " + (latest.inVehicle() ? "in a vehicle" : "on foot") + " for " + latest.getDurationFormatted());

        long drivingDuration = 0;
        if (latest.status == ActivityTrackerService.STATUS_IN_A_VEHICLE) {
            drivingDuration = latest.getDurationMillis();
        }

        showTimeDriven(drivingDuration);
    }

    private void showTimeDriven(long drivingDuration) {
        updateDrivingProgressBar(drivingDuration);
        showWarningLevelMessage(getCurrentLevel(drivingDuration));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tts = new TextToSpeech(getApplicationContext(), new TTSInitListener());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView output = (TextView)findViewById(R.id.current_state);
        output.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak("You have been driving for over " +
                        WarningLevel.DANGER.timeDesc + ". " +
                        WarningLevel.DANGER.message + ". " +
                        "There is a rest stop in 2 km. Perhaps stop there?"
                        , TextToSpeech.QUEUE_ADD, null);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(ActivityTrackerService.BROADCAST_ACTION));

        new ActivityTrackerScan(this).startActivityRecognitionScan();
        showWarningLevelMessage(WarningLevel.NOT_DRIVING);
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
