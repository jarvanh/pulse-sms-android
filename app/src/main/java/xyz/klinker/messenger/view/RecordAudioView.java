/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.listener.AudioRecordedListener;

/**
 * View that allows you to record audio samples and provides a callback to put them back into the
 * ui by their content uri.
 */
@SuppressLint("ViewConstructor")
public class RecordAudioView extends FrameLayout {

    private static final String TAG = "RecordAudioView";

    private AudioRecordedListener listener;
    private boolean recording = false;
    private FloatingActionButton record;
    private TextView text;
    private int seconds;
    private int minutes;
    private String fileName;
    private MediaRecorder recorder;

    public RecordAudioView(Context context, AudioRecordedListener listener, int color) {
        super(context);

        this.listener = listener;
        init(color);
    }

    private void init(int color) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_record_audio, this, true);

        text = (TextView) findViewById(R.id.record_text);
        record = (FloatingActionButton) findViewById(R.id.record);
        record.setBackgroundTintList(ColorStateList.valueOf(color));
        record.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                recording = !recording;

                if (recording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
    }

    private String getFileName() {
        return getContext().getFilesDir() + "/Audio_" + System.currentTimeMillis() + ".mp4";
    }

    private void updateTimer() {
        if (recording) {
            String s = "" + seconds;
            if (s.length() == 1) {
                s = "0" + s;
            }

            String t = minutes + ":" + s;
            text.setText(t);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    seconds++;

                    if (seconds >= 60) {
                        seconds = 0;
                        minutes++;
                    }

                    updateTimer();
                }
            }, 1000);
        }
    }

    private void startRecording() {
        minutes = 0;
        seconds = 0;
        record.setImageResource(R.drawable.ic_stop);

        fileName = getFileName();
        File file = new File(fileName);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
        updateTimer();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        record.setImageResource(R.drawable.ic_record_audio);
        text.setText(R.string.start_recording_audio);

        if (listener != null) {
            File file = new File(fileName);
            Log.v(TAG, "saved to file " + fileName + " with size " +
                    file.length());
            if (file.length() != 0) {
                listener.onRecorded(Uri.fromFile(new File(fileName)));
            } else {
                Toast.makeText(getContext(), R.string.audio_recording_error, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

}
