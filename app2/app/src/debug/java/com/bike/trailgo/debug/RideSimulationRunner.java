package com.bike.trailgo.debug;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

/** Plays timestamped RideTestInput records at a selected rate; a future replay can use this unchanged. */
final class RideSimulationRunner {
    interface Listener {
        void onLocation(Location location, long simulationTimestampMs);
        void onPause(long simulationTimestampMs);
        void onResume(long simulationTimestampMs);
        void onFinished();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private List<RideTestInput> inputs;
    private Listener listener;
    private int speedMultiplier;
    private int index;

    void start(List<RideTestInput> inputs, int speedMultiplier, Listener listener) {
        stop();
        this.inputs = inputs;
        this.speedMultiplier = Math.max(1, speedMultiplier);
        this.listener = listener;
        this.running = inputs != null && !inputs.isEmpty();
        this.index = 0;
        if (running) playNext();
    }

    void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void playNext() {
        if (!running || inputs == null || index >= inputs.size()) {
            running = false;
            if (listener != null) listener.onFinished();
            return;
        }
        RideTestInput input = inputs.get(index++);
        if (listener != null) {
            if (input.type == RideTestInput.Type.LOCATION) listener.onLocation(input.toLocation(), input.timestampMs);
            else if (input.type == RideTestInput.Type.PAUSE) listener.onPause(input.timestampMs);
            else listener.onResume(input.timestampMs);
        }
        long delayMillis = 100L;
        if (index < inputs.size()) {
            delayMillis = Math.max(100L, (inputs.get(index).timestampMs - input.timestampMs) / speedMultiplier);
        }
        handler.postDelayed(this::playNext, delayMillis);
    }
}
