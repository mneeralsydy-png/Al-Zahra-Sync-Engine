package com.alzahra.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";
    private static MediaRecorder recorder;
    private static boolean isRecording = false;
    private static String currentNumber = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (number != null && !number.isEmpty()) {
            currentNumber = number;
        }

        SharedPreferences prefs = context.getSharedPreferences("alzahra_settings", 
            Context.MODE_PRIVATE);
        boolean recordingEnabled = prefs.getBoolean("call_recording", true);

        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            if (recordingEnabled) {
                startRecording(context);
            }
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            stopRecording();
        }
    }

    private void startRecording(Context context) {
        if (isRecording) return;

        try {
            File dir = new File(context.getFilesDir(), "recordings");
            if (!dir.exists()) dir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", 
                Locale.getDefault()).format(new Date());
            File file = new File(dir, "CALL_" + currentNumber + "_" + timestamp + ".mp3");

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(file.getAbsolutePath());

            recorder.prepare();
            recorder.start();
            isRecording = true;
            Log.d(TAG, "Recording started: " + file.getName());

        } catch (IOException e) {
            Log.e(TAG, "Recording error", e);
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
            Log.d(TAG, "Recording stopped");
        } catch (Exception e) {
            Log.e(TAG, "Stop error", e);
        }
    }
}
