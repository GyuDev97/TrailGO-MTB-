package com.bike.trailgo.rideassistant;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** One service-scoped TTS instance; safety messages may interrupt lower-priority guidance. */
public final class RideVoiceManager implements TextToSpeech.OnInitListener {
    private final Context appContext;
    private TextToSpeech tts;
    private boolean ready;
    private PendingMessage pending;

    public RideVoiceManager(Context context) {
        appContext = context.getApplicationContext();
    }

    public synchronized void start() {
        if (tts == null) tts = new TextToSpeech(appContext, this);
    }

    public synchronized void speak(String message, VoicePriority priority) {
        if (message == null || message.trim().isEmpty()) return;
        if (!ready || tts == null) {
            if (pending == null || priority.ordinal() >= pending.priority.ordinal()) {
                pending = new PendingMessage(message, priority);
            }
            start();
            return;
        }
        int queueMode = priority.ordinal() >= VoicePriority.HIGH.ordinal()
                ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
        tts.speak(message, queueMode, null, "ride-assistant-" + System.currentTimeMillis());
    }

    @Override
    public synchronized void onInit(int status) {
        ready = status == TextToSpeech.SUCCESS;
        if (!ready || tts == null) return;
        tts.setLanguage(Locale.KOREAN);
        tts.setSpeechRate(1.0f);
        if (pending != null) {
            PendingMessage message = pending;
            pending = null;
            speak(message.message, message.priority);
        }
    }

    public synchronized void shutdown() {
        pending = null;
        ready = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    public synchronized void stop() {
        pending = null;
        if (tts != null) {
            tts.stop();
        }
    }

    private static final class PendingMessage {
        final String message;
        final VoicePriority priority;

        PendingMessage(String message, VoicePriority priority) {
            this.message = message;
            this.priority = priority;
        }
    }
}
