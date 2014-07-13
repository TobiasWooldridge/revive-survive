package tw.tobias.reviveandsurvive;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import tw.tobias.reviveandsurvive.client.CrashStatsClient;
import tw.tobias.reviveandsurvive.client.PitStopClient;
import tw.tobias.reviveandsurvive.client.PitStop;

import java.io.IOException;
import java.util.*;

public class MainActivity extends Activity {
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showDrivingState(intent);
        }
    };

    private final static String TAG = "MainActivity";

    private final static int ONE_HOUR = 60 * 60 * 1000;
    private final static int TWO_HOURS = 2 * ONE_HOUR;
    private TextToSpeech tts;
    private PitStopClient pitStopClient;
    private CrashStatsClient crashStatsClient;
    private ListView listView;

    List<PitStop> pitStops = Collections.emptyList();
    int numCrashes = 0;

    int[] checkboxIds = { R.id.button_petrol_stations, R.id.button_public_rest_stops, R.id.button_public_toilets };

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

        SHORT_DRIVE(1, "You haven't been driving for very long. No problem! Keep your eyes on the road.", "One minute"),

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

    static RiskLevel[] riskLevels = {
            RiskLevel.CITY_DRIVING,
            RiskLevel.HIGH_RISK,
            RiskLevel.MEDIUM_RISK,
            RiskLevel.LOW_RISK
    };


    public RiskLevel getRiskLevel(int crashes) {
        for (RiskLevel level : riskLevels) {
            if (crashes >= level.threshold) {
                return level;
            }
        }
        return RiskLevel.LOW_RISK;
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
                    PitStop next = pitStops.get(0);

                    tts.speak("You have been driving for over " +
                            level.timeDesc + ". " +
                            level.message + ". " +
                            String.format("There is a %s in %s. You could stop there.", next.getType(), next.getDistanceReadable())
                            , TextToSpeech.QUEUE_ADD, null);
                    break;
                default:
                    break;
            }
        }

        previousLevel = level;
    }

    public void showRiskLevelMessage(RiskLevel level) {
        TextView riskMessage = (TextView)findViewById(R.id.risk_level);
        riskMessage.setText(String.format("You are in a %s (%d accidents in 5 years)", level.message, numCrashes));
    }

    public void updateDrivingProgressBar(long drivingDuration) {
        SeekBar drivingDurationBar = (SeekBar)findViewById(R.id.driving_duration_status_bar);
        drivingDurationBar.setEnabled(false);
        int progressPercent = (int)(100 * ((double)drivingDuration / TWO_HOURS  ));
        Log.i(TAG, "Driving for " + drivingDuration + " aka " + progressPercent + "%");
        drivingDurationBar.setProgress(Math.min(100, progressPercent));
    }

    private void showDrivingState(Intent intent) {
        TextView output = (TextView)findViewById(R.id.current_state);
        TextView topText = (TextView)findViewById(R.id.you_have_been_driving);

        Measurement latest = Measurement.fromJson(intent.getStringExtra("latestMeasurement"));

        output.setText("You have been " + (latest.inVehicle() ? "in a vehicle" : "on foot") + " for " + latest.getDurationFormatted());

        long drivingDuration = 0;
        if (latest.status.equals(ActivityTrackerService.STATUS_IN_A_VEHICLE)) {
            drivingDuration = latest.getDurationMillis();

            topText.setText("You have been driving for " + latest.getDurationFormatted());
        }
        else {
            topText.setText("You are not driving at the moment");
        }


        showTimeDriven(drivingDuration);
    }

    private void refreshData() {
        new GetPointsTask().execute();
        new GetCrashesTask().execute();
    }

    private void showTimeDriven(long drivingDuration) {
        updateDrivingProgressBar(drivingDuration);
        showWarningLevelMessage(getCurrentLevel(drivingDuration));
    }

    private class GetPointsTask extends AsyncTask<Void, Void, List<PitStop>> {
        @Override
        protected List<PitStop> doInBackground(Void... voids) {
            try {
                return pitStopClient.getStops(-34.9274606, 138.6008726, 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Collections.emptyList();
        }

        @Override
        protected void onPostExecute(List<PitStop> newPitStops) {
            super.onPostExecute(newPitStops);

            pitStops = newPitStops;
            displayPitStops();
        }
    }

    private class GetCrashesTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                return crashStatsClient.getNumAccidents(-34.9274606, 138.6008726, 5000);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
            }

            return 1207;
        }

        @Override
        protected void onPostExecute(Integer newNumCrashes) {
            super.onPostExecute(newNumCrashes);

            numCrashes = newNumCrashes;
            showRiskLevelMessage(getRiskLevel(numCrashes));
        }
    }

    private boolean isChecked(int id) {
        CheckBox box = (CheckBox)findViewById(id);
        return box.isChecked();
    }

    private void displayPitStops() {
        if (listView == null) {
            listView = (ListView) findViewById(R.id.pit_stops);
        }

        Set<String> filterSet = new HashSet<>();
        if (isChecked(R.id.button_petrol_stations)) {
            filterSet.add(PitStop.PETROL_TYPE);
        }
        if (isChecked(R.id.button_public_rest_stops)) {
            filterSet.add(PitStop.REST_TYPE);
        }
        if (isChecked(R.id.button_public_toilets)) {
            filterSet.add(PitStop.TOILET_TYPE);
        }

        List<String> values = new ArrayList<>();
        final ArrayList<PitStop> pitStopList = new ArrayList<>();
        for (PitStop ps : pitStops) {
            if (filterSet.contains(ps.getRawType())) {
                values.add(ps.toString());
                pitStopList.add(ps);
            }
        }


        PitStopAdapter adapter = new PitStopAdapter(this, pitStopList);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PitStop ps = pitStopList.get(position);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + ps.getLat() + "," + ps.getLon()));
                startActivity(i);
            }
        });
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
                PitStop next = pitStops.get(0);
                tts.speak("You have been driving for over " +
                        WarningLevel.DANGER.timeDesc + ". " +
                        WarningLevel.DANGER.message + ". " +
                        String.format("There is a %s in %s. You could stop there.", next.getType(), next.getDistanceReadable())
                        , TextToSpeech.QUEUE_ADD, null);
            }
        });

        // Load nearby points of interest (stops, toilets, etc.)
        pitStopClient = new PitStopClient();

        // Get how many crashes have been nearby in the last 5 years
        crashStatsClient = new CrashStatsClient();

        refreshData();

        showWarningLevelMessage(WarningLevel.NOT_DRIVING);
        for (int id : checkboxIds) {
            CheckBox checkBox = (CheckBox)findViewById(id);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayPitStops();
                }
            });
        }
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

        if (id == R.id.refresh) {
            refreshData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
