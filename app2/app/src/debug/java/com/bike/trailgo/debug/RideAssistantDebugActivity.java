package com.bike.trailgo.debug;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bike.trailgo.R;
import com.bike.trailgo.rideassistant.RideAssistant;
import com.bike.trailgo.rideassistant.RideContext;
import com.bike.trailgo.rideassistant.RideEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Debug-only desk test screen. It emits Locations into the production RideAssistant pipeline. */
public final class RideAssistantDebugActivity extends AppCompatActivity {
    private static final String TAG = "RideSimulator";
    private Spinner scenarioSpinner;
    private Spinner speedSpinner;
    private Switch gyuvisSwitch;
    private TextView status;
    private TextView simulationTime;
    private TextView currentSpeed;
    private TextView currentAltitude;
    private TextView estimatedGrade;
    private TextView averages;
    private TextView elevationGain;
    private TextView durations;
    private TextView rideState;
    private TextView lastEvent;
    private TextView gyuvisStatus;
    private TextView speakStatus;
    private RideAssistant assistant;
    private RideSimulationRunner runner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_assistant_debug);
        scenarioSpinner = findViewById(R.id.sim_scenario);
        speedSpinner = findViewById(R.id.sim_speed);
        gyuvisSwitch = findViewById(R.id.sim_gyuvis);
        status = findViewById(R.id.sim_status);
        simulationTime = findViewById(R.id.sim_time);
        currentSpeed = findViewById(R.id.sim_current_speed);
        currentAltitude = findViewById(R.id.sim_current_altitude);
        estimatedGrade = findViewById(R.id.sim_grade);
        averages = findViewById(R.id.sim_averages);
        elevationGain = findViewById(R.id.sim_elevation_gain);
        durations = findViewById(R.id.sim_durations);
        rideState = findViewById(R.id.sim_state);
        lastEvent = findViewById(R.id.sim_event);
        gyuvisStatus = findViewById(R.id.sim_gyuvis_status);
        speakStatus = findViewById(R.id.sim_speak);

        String[] scenarios = new String[RideScenario.values().length];
        for (int index = 0; index < RideScenario.values().length; index++) scenarios[index] = RideScenario.values()[index].label;
        scenarioSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, scenarios));
        speedSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"1x", "10x", "30x", "60x"}));
        speedSpinner.setSelection(3);

        ((Button) findViewById(R.id.sim_start)).setOnClickListener(v -> startSimulation());
        ((Button) findViewById(R.id.sim_stop)).setOnClickListener(v -> stopSimulation("SIMULATION STOPPED"));
        ((Button) findViewById(R.id.sim_reset)).setOnClickListener(v -> resetScreen());
        findViewById(R.id.sim_close).setOnClickListener(v -> finish());
        resetScreen();
    }

    private void startSimulation() {
        stopSimulation(null);
        resetScreen();
        boolean gyuvisEnabled = gyuvisSwitch.isChecked();
        assistant = new RideAssistant(this, gyuvisEnabled, new RideAssistant.Observer() {
            @Override
            public void onContext(RideContext context) {
                runOnUiThread(() -> renderContext(context));
            }

            @Override
            public void onEvent(RideEvent event, String reason) {
                runOnUiThread(() -> lastEvent.setText(event.type + "\n" + reason));
            }

            @Override
            public void onGyuvisStatus(RideEvent event, String requestStatus, boolean speak,
                                       String priority, String message) {
                runOnUiThread(() -> {
                    gyuvisStatus.setText(requestStatus + "\n" + event.type);
                    speakStatus.setText(speak ? "TRUE  " + safe(priority) + "\n" + safe(message) : "FALSE");
                });
            }
        });
        assistant.start();
        runner = new RideSimulationRunner();
        RideScenario scenario = RideScenario.fromLabel(String.valueOf(scenarioSpinner.getSelectedItem()));
        int multiplier = parseMultiplier(String.valueOf(speedSpinner.getSelectedItem()));
        status.setText("SIMULATION RUNNING · " + scenario.label + " · " + multiplier + "x");
        runner.start(scenario.create(System.currentTimeMillis()), multiplier, new RideSimulationRunner.Listener() {
            @Override
            public void onLocation(android.location.Location location, long simulationTimestampMs) {
                Log.d(TAG, "[SIM] timestamp=" + simulationTimestampMs + " speed="
                        + (location.getSpeed() * 3.6f) + " altitude=" + location.getAltitude());
                assistant.onLocation(location);
                simulationTime.setText(formatTime(simulationTimestampMs));
            }

            @Override
            public void onPause(long simulationTimestampMs) {
                assistant.pause(simulationTimestampMs);
                status.setText("SIMULATION PAUSED");
            }

            @Override
            public void onResume(long simulationTimestampMs) {
                assistant.resume(simulationTimestampMs);
                status.setText("SIMULATION RUNNING · RESUMED");
            }

            @Override
            public void onFinished() {
                status.setText("SIMULATION COMPLETE");
            }
        });
    }

    private void renderContext(RideContext context) {
        currentSpeed.setText(format("%.1f km/h", context.currentSpeedKmh));
        currentAltitude.setText(format("%.1f m", context.currentAltitudeMeters));
        estimatedGrade.setText(format("%+.1f %%", context.estimatedGradePercent));
        averages.setText(format("1m  %.1f   5m  %.1f   10m  %.1f km/h",
                context.avgSpeed1mKmh, context.avgSpeed5mKmh, context.avgSpeed10mKmh));
        elevationGain.setText(format("%.1f m (10분) / 누적 %.1f m",
                context.elevationGain10mMeters, context.totalElevationGainMeters));
        durations.setText("상승 " + duration(context.continuousClimbSeconds)
                + " / 하강 " + duration(context.continuousDescentSeconds));
        rideState.setText(context.currentRideState.name());
    }

    private void stopSimulation(String message) {
        if (runner != null) runner.stop();
        runner = null;
        if (assistant != null) assistant.shutdown();
        assistant = null;
        if (message != null) status.setText(message);
    }

    private void resetScreen() {
        status.setText("SIMULATION READY");
        simulationTime.setText("-");
        currentSpeed.setText("-");
        currentAltitude.setText("-");
        estimatedGrade.setText("-");
        averages.setText("-");
        elevationGain.setText("-");
        durations.setText("-");
        rideState.setText("-");
        lastEvent.setText("없음");
        gyuvisStatus.setText(gyuvisSwitch != null && gyuvisSwitch.isChecked() ? "대기" : "OFF");
        speakStatus.setText("-");
    }

    private static int parseMultiplier(String value) {
        try {
            return Integer.parseInt(value.replace("x", ""));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String formatTime(long timestampMs) {
        return new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date(timestampMs));
    }

    private static String duration(long seconds) {
        return String.format(Locale.KOREA, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String format(String pattern, Object... values) {
        return String.format(Locale.KOREA, pattern, values);
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    @Override
    protected void onDestroy() {
        stopSimulation(null);
        super.onDestroy();
    }
}
