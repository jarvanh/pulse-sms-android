/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.io.File
import java.io.IOException

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.util.listener.AudioRecordedListener

/**
 * View that allows you to record audio samples and provides a callback to put them back into the
 * ui by their content uri.
 */
@SuppressLint("ViewConstructor")
class RecordAudioView(context: Context, private val listener: AudioRecordedListener?, color: Int) : FrameLayout(context) {

    private val record: FloatingActionButton by lazy { findViewById<View>(R.id.record) as FloatingActionButton }
    private val text: TextView by lazy { findViewById<View>(R.id.record_text) as TextView }

    private var recording = false
    private var seconds: Int = 0
    private var minutes: Int = 0

    private val fileName: String by lazy { context.filesDir.toString() + "/Audio_" + TimeUtils.now + ".mp4" }
    private var recorder: MediaRecorder? = null

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_record_audio, this, true)

        record.backgroundTintList = ColorStateList.valueOf(color)
        record.setOnClickListener {
            recording = !recording

            if (recording) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    private fun updateTimer() {
        if (recording) {
            var s = "" + seconds
            if (s.length == 1) {
                s = "0" + s
            }

            val t = minutes.toString() + ":" + s
            text.text = t

            Handler().postDelayed({
                seconds++

                if (seconds >= 60) {
                    seconds = 0
                    minutes++
                }

                updateTimer()
            }, 1000)
        }
    }

    private fun startRecording() {
        minutes = 0
        seconds = 0
        record.setImageResource(R.drawable.ic_stop)

        val file = File(fileName)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        recorder = MediaRecorder()
        recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder?.setOutputFile(fileName)
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)

        try {
            recorder?.prepare()
            recorder?.start()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "prepare() failed")
        } catch (e: IOException) {
            Log.e(TAG, "prepare() failed")
        }

        updateTimer()
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null

            record.setImageResource(R.drawable.ic_record_audio)
            text.setText(R.string.start_recording_audio)

            if (listener != null) {
                val file = File(fileName)
                Log.v(TAG, "saved to file " + fileName + " with size " +
                        file.length())
                if (file.length() != 0L) {
                    listener.onRecorded(Uri.fromFile(File(fileName)))
                } else {
                    Toast.makeText(context, R.string.audio_recording_error, Toast.LENGTH_SHORT)
                            .show()
                }
            }
        } catch (e: Exception) {
        }
    }

    companion object {
        private val TAG = "RecordAudioView"
    }
}
